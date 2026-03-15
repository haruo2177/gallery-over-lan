package com.example.galleryoverlan.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.usecase.BrowseFoldersUseCase
import com.example.galleryoverlan.domain.usecase.ListImagesUseCase
import com.example.galleryoverlan.domain.usecase.SearchFoldersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val browseFoldersUseCase: BrowseFoldersUseCase,
    private val searchFoldersUseCase: SearchFoldersUseCase,
    private val listImagesUseCase: ListImagesUseCase,
    private val smbRepository: SmbRepository,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var baseFolderPath: String = ""

    init {
        connectAndLoad()
    }

    private fun connectAndLoad() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            val connectResult = smbRepository.connectWithSavedConfig()
            if (connectResult is AppResult.Error) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = connectResult.message
                )
                return@launch
            }

            val config = settingsRepository.connectionConfig.first()
            baseFolderPath = config?.baseFolderPath ?: ""

            _uiState.value = _uiState.value.copy(isConnecting = false)
            navigateTo(baseFolderPath)
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                searchQuery = "",
                currentPath = path
            )

            val breadcrumbs = buildBreadcrumbs(path)

            val foldersResult = browseFoldersUseCase(path)
            val imageCountResult = listImagesUseCase(path)
            val imageCount = when (imageCountResult) {
                is AppResult.Success -> imageCountResult.data.size
                is AppResult.Error -> 0
            }

            when (foldersResult) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        folders = foldersResult.data,
                        filteredFolders = foldersResult.data,
                        imageCount = imageCount,
                        breadcrumbs = breadcrumbs,
                        isLoading = false
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = foldersResult.message
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatchers.default) {
            delay(SEARCH_DEBOUNCE_MS)
            val filtered = searchFoldersUseCase(_uiState.value.folders, query)
            _uiState.value = _uiState.value.copy(filteredFolders = filtered)
        }
    }

    fun refresh() {
        navigateTo(_uiState.value.currentPath)
    }

    private fun buildBreadcrumbs(path: String): List<BreadcrumbItem> {
        if (path.isEmpty()) return listOf(BreadcrumbItem("Root", ""))

        val parts = path.split("/").filter { it.isNotEmpty() }
        val breadcrumbs = mutableListOf(BreadcrumbItem("Root", ""))
        var accumulated = ""
        for (part in parts) {
            accumulated = if (accumulated.isEmpty()) part else "$accumulated/$part"
            breadcrumbs.add(BreadcrumbItem(part, accumulated))
        }
        return breadcrumbs
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(dispatchers.io) {
            smbRepository.disconnect()
        }
    }
}
