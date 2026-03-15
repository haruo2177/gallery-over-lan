package com.example.galleryoverlan.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.usecase.ListImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageListViewModel @Inject constructor(
    private val listImagesUseCase: ListImagesUseCase,
    private val smbRepository: SmbRepository,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageListUiState())
    val uiState: StateFlow<ImageListUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
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
            val folderPath = config?.baseFolderPath ?: ""

            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isLoading = true,
                folderPath = folderPath
            )

            val result = listImagesUseCase(folderPath)
            _uiState.value = when (result) {
                is AppResult.Success -> _uiState.value.copy(
                    images = result.data,
                    isLoading = false,
                    error = null
                )
                is AppResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(dispatchers.io) {
            smbRepository.disconnect()
        }
    }
}
