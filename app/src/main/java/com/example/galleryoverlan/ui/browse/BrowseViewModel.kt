package com.example.galleryoverlan.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.search.SearchHistoryEntry
import com.example.galleryoverlan.data.search.SearchHistoryRepository
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SearchMode
import com.example.galleryoverlan.domain.model.SearchOptions
import com.example.galleryoverlan.domain.model.SortOrder
import com.example.galleryoverlan.domain.usecase.BrowseFoldersUseCase
import com.example.galleryoverlan.domain.usecase.ConnectToShareUseCase
import com.example.galleryoverlan.domain.usecase.ListImagesUseCase
import com.example.galleryoverlan.domain.usecase.ListSharesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val listSharesUseCase: ListSharesUseCase,
    private val connectToShareUseCase: ConnectToShareUseCase,
    private val browseFoldersUseCase: BrowseFoldersUseCase,
    private val listImagesUseCase: ListImagesUseCase,
    private val smbRepository: SmbRepository,
    private val dispatchers: AppDispatchers,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val _navigateToViewer = MutableSharedFlow<Pair<String, Int>>()
    val navigateToViewer: SharedFlow<Pair<String, Int>> = _navigateToViewer.asSharedFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val scrollPositionCache = mutableMapOf<String, Int>()
    private val searchStateCache = mutableMapOf<String, CachedSearchState>()

    init {
        loadShares()
    }

    private fun loadShares() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            doLoadShares()
        }
    }

    private suspend fun doLoadShares() {
        when (val result = listSharesUseCase()) {
            is AppResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    level = BrowseLevel.Shares,
                    shares = result.data,
                    breadcrumbs = listOf(BrowseBreadcrumbItem("共有一覧", "")),
                    isLoading = false,
                    error = null
                )
            }
            is AppResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun onShareSelected(shareName: String) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = connectToShareUseCase(shareName)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentShareName = shareName,
                        level = BrowseLevel.Folder,
                        currentPath = ""
                    )
                    loadFolderContents("")
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun saveScrollPosition(firstVisibleItemIndex: Int) {
        val state = _uiState.value
        scrollPositionCache[state.currentPath] = firstVisibleItemIndex
        if (state.isSearchActive) {
            searchStateCache[state.currentPath] = CachedSearchState(
                query = state.searchQuery,
                options = state.searchOptions
            )
        } else {
            searchStateCache.remove(state.currentPath)
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(
                currentPath = path,
                isLoading = true
            )
            loadFolderContents(path)
        }
    }

    private suspend fun loadFolderContents(path: String) {
        val foldersResult = browseFoldersUseCase(path)
        val imagesResult = listImagesUseCase(path)

        val folders = when (foldersResult) {
            is AppResult.Success -> foldersResult.data
            is AppResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = foldersResult.message
                )
                return
            }
        }

        val images: List<ImageItem>
        val imageLoadError: String?
        when (imagesResult) {
            is AppResult.Success -> {
                images = sortImages(imagesResult.data, _uiState.value.sortOrder)
                imageLoadError = null
            }
            is AppResult.Error -> {
                images = emptyList()
                imageLoadError = imagesResult.message
            }
        }

        val cachedSearch = searchStateCache[path]
        val filteredResult = if (cachedSearch != null) {
            applySearch(folders, images, cachedSearch.query, cachedSearch.options)
        } else {
            null
        }

        _uiState.value = _uiState.value.copy(
            folders = folders,
            images = images,
            imageLoadError = imageLoadError,
            breadcrumbs = buildBreadcrumbs(path),
            isLoading = false,
            error = null,
            targetScrollIndex = scrollPositionCache[path] ?: 0,
            filteredFolders = filteredResult?.first,
            filteredImages = filteredResult?.second,
            isSearchActive = cachedSearch != null,
            searchQuery = cachedSearch?.query ?: ""
        )
    }

    fun onImageClick(imageIndex: Int) {
        val state = _uiState.value
        val actualIndex = if (state.isSearchActive && state.filteredImages != null) {
            val clickedImage = state.filteredImages[imageIndex]
            state.images.indexOf(clickedImage)
        } else {
            imageIndex
        }
        viewModelScope.launch {
            _navigateToViewer.emit(state.currentPath to actualIndex)
        }
    }

    fun onBreadcrumbClick(item: BrowseBreadcrumbItem) {
        if (item.path.isEmpty() && _uiState.value.level == BrowseLevel.Folder &&
            _uiState.value.currentPath.isEmpty()
        ) {
            goBackToShares()
            return
        }
        if (item.name == "共有一覧") {
            goBackToShares()
            return
        }
        navigateTo(item.path)
    }

    fun onBackPressed(): Boolean {
        val state = _uiState.value
        return when {
            state.level == BrowseLevel.Folder && state.currentPath.isNotEmpty() -> {
                val parentPath = state.currentPath.substringBeforeLast("/", "")
                navigateTo(parentPath)
                true
            }
            state.level == BrowseLevel.Folder && state.currentPath.isEmpty() -> {
                goBackToShares()
                true
            }
            else -> false
        }
    }

    private fun goBackToShares() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(
                level = BrowseLevel.Shares,
                currentShareName = "",
                currentPath = "",
                folders = emptyList(),
                images = emptyList(),
                breadcrumbs = listOf(BrowseBreadcrumbItem("共有一覧", "")),
                showSearchPanel = false,
                isSearchActive = false,
                searchQuery = "",
                filteredFolders = null,
                filteredImages = null
            )
            loadShares()
        }
    }

    fun refresh() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch(dispatchers.io) {
            delay(500)
            when (state.level) {
                is BrowseLevel.Shares -> doLoadShares()
                is BrowseLevel.Folder -> loadFolderContents(state.currentPath)
            }
        }
    }

    // Search

    fun toggleSearchPanel() {
        val state = _uiState.value
        val newShow = !state.showSearchPanel
        _uiState.value = state.copy(
            showSearchPanel = newShow,
            searchHistory = if (newShow) searchHistoryRepository.getHistory() else state.searchHistory
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onSearchOptionsChange(options: SearchOptions) {
        _uiState.value = _uiState.value.copy(searchOptions = options)
    }

    fun executeSearch() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        if (query.isEmpty() && state.searchOptions.excludeQuery.isBlank()) {
            clearSearch()
            return
        }

        searchHistoryRepository.addEntry(query, state.searchOptions)

        val filtered = applySearch(state.folders, state.images, query, state.searchOptions)
        _uiState.value = state.copy(
            filteredFolders = filtered.first,
            filteredImages = filtered.second,
            isSearchActive = true,
            showSearchPanel = false,
            searchHistory = searchHistoryRepository.getHistory()
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            filteredFolders = null,
            filteredImages = null,
            isSearchActive = false,
            searchQuery = "",
            searchOptions = _uiState.value.searchOptions.copy(excludeQuery = "")
        )
    }

    fun onHistoryItemClick(entry: SearchHistoryEntry) {
        _uiState.value = _uiState.value.copy(
            searchQuery = entry.query,
            searchOptions = entry.options
        )
    }

    fun onHistoryItemDelete(query: String) {
        searchHistoryRepository.removeEntry(query)
        _uiState.value = _uiState.value.copy(
            searchHistory = searchHistoryRepository.getHistory()
        )
    }

    fun clearSearchHistory() {
        searchHistoryRepository.clearHistory()
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
    }

    private fun applySearch(
        folders: List<FolderItem>,
        images: List<ImageItem>,
        query: String,
        options: SearchOptions
    ): Pair<List<FolderItem>, List<ImageItem>> {
        val matchFn = buildMatchFunction(query, options)
        val excludeFn = if (options.excludeQuery.isNotBlank()) {
            buildMatchFunction(options.excludeQuery, options.copy(excludeQuery = "", mode = SearchMode.OR))
        } else {
            null
        }

        val filteredFolders = folders.filter { folder ->
            val matches = query.isEmpty() || matchFn(folder.name)
            val excluded = excludeFn?.invoke(folder.name) ?: false
            matches && !excluded
        }

        val filteredImages = images.filter { image ->
            val matches = query.isEmpty() || matchFn(image.name)
            val excluded = excludeFn?.invoke(image.name) ?: false
            matches && !excluded
        }

        return filteredFolders to filteredImages
    }

    private fun buildMatchFunction(query: String, options: SearchOptions): (String) -> Boolean {
        val keywords = query.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return { true }

        val matchers: List<(String) -> Boolean> = keywords.map { kw ->
            val normalizedKw = normalizeIfNeeded(kw, options)
            if (options.useWildcard && (normalizedKw.contains('*') || normalizedKw.contains('?'))) {
                val regexPattern = buildString {
                    append("^")
                    for (ch in normalizedKw) {
                        when (ch) {
                            '*' -> append(".*")
                            '?' -> append(".")
                            else -> append(Regex.escape(ch.toString()))
                        }
                    }
                    append("$")
                }
                val regexOpts = if (options.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                val wcRegex = Regex(regexPattern, regexOpts)
                val fn: (String) -> Boolean = { text -> wcRegex.containsMatchIn(normalizeIfNeeded(text, options)) }
                fn
            } else {
                val fn: (String) -> Boolean = { text ->
                    val normalizedText = normalizeIfNeeded(text, options)
                    if (options.caseSensitive) normalizedText.contains(normalizedKw)
                    else normalizedText.lowercase().contains(normalizedKw.lowercase())
                }
                fn
            }
        }

        return { text ->
            when (options.mode) {
                SearchMode.AND -> matchers.all { it(text) }
                SearchMode.OR -> matchers.any { it(text) }
            }
        }
    }

    private fun normalizeIfNeeded(text: String, options: SearchOptions): String {
        if (!options.kanaUnify) return text
        return text.map { ch ->
            when (ch) {
                in '\u3041'..'\u3096' -> ch + 0x60 // hiragana -> katakana
                else -> ch
            }
        }.joinToString("")
    }

    private fun buildBreadcrumbs(path: String): List<BrowseBreadcrumbItem> {
        val items = mutableListOf(BrowseBreadcrumbItem("共有一覧", ""))
        items.add(BrowseBreadcrumbItem(_uiState.value.currentShareName, ""))

        if (path.isNotEmpty()) {
            val parts = path.split("/").filter { it.isNotEmpty() }
            var accumulated = ""
            for (part in parts) {
                accumulated = if (accumulated.isEmpty()) part else "$accumulated/$part"
                items.add(BrowseBreadcrumbItem(part, accumulated))
            }
        }
        return items
    }

    private fun sortImages(images: List<ImageItem>, order: SortOrder): List<ImageItem> {
        return when (order) {
            SortOrder.NAME_ASC -> images.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> images.sortedByDescending { it.name.lowercase() }
            SortOrder.DATE_ASC -> images.sortedBy { it.lastModified }
            SortOrder.DATE_DESC -> images.sortedByDescending { it.lastModified }
            SortOrder.SIZE_ASC -> images.sortedBy { it.sizeBytes }
            SortOrder.SIZE_DESC -> images.sortedByDescending { it.sizeBytes }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(dispatchers.io) {
            smbRepository.disconnect()
        }
    }
}
