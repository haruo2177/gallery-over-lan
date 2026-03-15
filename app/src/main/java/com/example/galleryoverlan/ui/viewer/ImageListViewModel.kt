package com.example.galleryoverlan.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SortOrder
import com.example.galleryoverlan.domain.usecase.ListImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listImagesUseCase: ListImagesUseCase,
    private val smbRepository: SmbRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val folderPath: String = (savedStateHandle.get<String>("folderPath") ?: "").trim()
    private var allImages: List<ImageItem> = emptyList()

    private val _uiState = MutableStateFlow(ImageListUiState(folderPath = folderPath))
    val uiState: StateFlow<ImageListUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch(dispatchers.io) {
            if (!smbRepository.isConnected()) {
                _uiState.value = _uiState.value.copy(isConnecting = true, error = null)
                val connectResult = smbRepository.connectWithSavedConfig()
                if (connectResult is AppResult.Error) {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = connectResult.message
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(isConnecting = false)
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = listImagesUseCase(folderPath)
            when (result) {
                is AppResult.Success -> {
                    allImages = result.data
                    _uiState.value = _uiState.value.copy(
                        images = sortImages(allImages, _uiState.value.sortOrder),
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
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        _uiState.value = _uiState.value.copy(
            sortOrder = sortOrder,
            images = sortImages(allImages, sortOrder),
            showSortMenu = false
        )
    }

    fun toggleSortMenu() {
        _uiState.value = _uiState.value.copy(showSortMenu = !_uiState.value.showSortMenu)
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
}
