package com.example.galleryoverlan.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.usecase.ListImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listImagesUseCase: ListImagesUseCase,
    private val smbRepository: SmbRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val folderPath: String = savedStateHandle["folderPath"] ?: ""
    private val startIndex: Int = savedStateHandle["startIndex"] ?: 0

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    private fun loadImages() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = listImagesUseCase(folderPath)
            _uiState.value = when (result) {
                is AppResult.Success -> _uiState.value.copy(
                    images = result.data,
                    currentIndex = startIndex.coerceIn(0, (result.data.size - 1).coerceAtLeast(0)),
                    isLoading = false
                )
                is AppResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }
}
