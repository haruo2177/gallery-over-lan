package com.example.galleryoverlan.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.ImageItem
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
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val _navigateToViewer = MutableSharedFlow<Pair<String, Int>>()
    val navigateToViewer: SharedFlow<Pair<String, Int>> = _navigateToViewer.asSharedFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val scrollPositionCache = mutableMapOf<String, Int>()

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
        scrollPositionCache[_uiState.value.currentPath] = firstVisibleItemIndex
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

        _uiState.value = _uiState.value.copy(
            folders = folders,
            images = images,
            imageLoadError = imageLoadError,
            breadcrumbs = buildBreadcrumbs(path),
            isLoading = false,
            error = null,
            targetScrollIndex = scrollPositionCache[path] ?: 0
        )
    }

    fun onImageClick(imageIndex: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            _navigateToViewer.emit(state.currentPath to imageIndex)
        }
    }

    fun onBreadcrumbClick(item: BrowseBreadcrumbItem) {
        if (item.path.isEmpty() && _uiState.value.level == BrowseLevel.Folder &&
            _uiState.value.currentPath.isEmpty()
        ) {
            // Already at share root, go back to shares
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
                breadcrumbs = listOf(BrowseBreadcrumbItem("共有一覧", ""))
            )
            loadShares()
        }
    }

    fun onSortOrderChange(order: SortOrder) {
        val state = _uiState.value
        _uiState.value = state.copy(
            sortOrder = order,
            showSortMenu = false,
            images = sortImages(state.images, order)
        )
    }

    fun toggleSortMenu() {
        _uiState.value = _uiState.value.copy(showSortMenu = !_uiState.value.showSortMenu)
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
