package com.example.galleryoverlan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.domain.usecase.SaveConnectionUseCase
import com.example.galleryoverlan.domain.usecase.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val testConnectionUseCase: TestConnectionUseCase,
    private val saveConnectionUseCase: SaveConnectionUseCase,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        viewModelScope.launch(dispatchers.io) {
            val config = settingsRepository.connectionConfig.first()
            if (config != null) {
                _uiState.value = _uiState.value.copy(
                    hostName = config.hostName,
                    shareName = config.shareName,
                    userName = config.userName,
                    baseFolderPath = config.baseFolderPath,
                    isSaved = true
                )
            }
        }
    }

    fun onHostNameChange(value: String) {
        _uiState.value = _uiState.value.copy(hostName = value, testResult = null)
    }

    fun onShareNameChange(value: String) {
        _uiState.value = _uiState.value.copy(shareName = value, testResult = null)
    }

    fun onUserNameChange(value: String) {
        _uiState.value = _uiState.value.copy(userName = value, testResult = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, testResult = null)
    }

    fun onBaseFolderPathChange(value: String) {
        _uiState.value = _uiState.value.copy(baseFolderPath = value, testResult = null)
    }

    fun testConnection() {
        val state = _uiState.value
        _uiState.value = state.copy(isTesting = true, testResult = null)

        viewModelScope.launch(dispatchers.io) {
            val result = testConnectionUseCase(
                host = state.hostName,
                shareName = state.shareName,
                userName = state.userName,
                password = state.password
            )
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = when (result) {
                    is AppResult.Success -> TestResult.Success
                    is AppResult.Error -> TestResult.Failure(result.message)
                }
            )
        }
    }

    fun saveSettings() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch(dispatchers.io) {
            saveConnectionUseCase(
                hostName = state.hostName,
                shareName = state.shareName,
                userName = state.userName,
                password = state.password,
                baseFolderPath = state.baseFolderPath
            )
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }
}
