package com.example.galleryoverlan.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.network.LanScanner
import com.example.galleryoverlan.data.security.CredentialRepository
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.domain.model.DiscoveredDevice
import com.example.galleryoverlan.domain.usecase.ConnectToHostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val connectToHostUseCase: ConnectToHostUseCase,
    private val settingsRepository: SettingsRepository,
    private val credentialRepository: CredentialRepository,
    private val lanScanner: LanScanner,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private var discoveryJob: Job? = null

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private val _navigateToBoowse = MutableSharedFlow<Unit>()
    val navigateToBrowse: SharedFlow<Unit> = _navigateToBoowse.asSharedFlow()

    init {
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        viewModelScope.launch(dispatchers.io) {
            val config = settingsRepository.connectionConfig.first()
            val credentials = credentialRepository.getCredentials()
            if (config != null) {
                _uiState.value = _uiState.value.copy(
                    pcName = config.hostName,
                    ipAddress = config.lastSuccessfulIp ?: "",
                    userName = config.userName,
                    password = credentials?.second ?: ""
                )
            }
        }
    }

    fun onPcNameChange(value: String) {
        _uiState.value = _uiState.value.copy(pcName = value, error = null)
    }

    fun onIpAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(ipAddress = value, error = null)
    }

    fun onUserNameChange(value: String) {
        _uiState.value = _uiState.value.copy(userName = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun connect() {
        val state = _uiState.value
        if (state.ipAddress.isBlank() && state.pcName.isBlank()) return
        if (state.userName.isBlank()) return

        _uiState.value = state.copy(isConnecting = true, error = null)

        viewModelScope.launch(dispatchers.io) {
            // Try IP first, then fall back to PC name
            val targets = buildList {
                if (state.ipAddress.isNotBlank()) add(state.ipAddress)
                if (state.pcName.isNotBlank() && state.pcName != state.ipAddress) add(state.pcName)
            }

            var lastError: String? = null
            for (target in targets) {
                val result = connectToHostUseCase(
                    hostName = target,
                    pcName = state.pcName,
                    userName = state.userName,
                    password = state.password
                )
                when (result) {
                    is AppResult.Success -> {
                        _navigateToBoowse.emit(Unit)
                        return@launch
                    }
                    is AppResult.Error -> {
                        lastError = result.message
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                error = lastError
            )
        }
    }

    fun startDiscovery() {
        discoveryJob?.cancel()
        _uiState.value = _uiState.value.copy(showDiscoveryDialog = true, discoveryState = null)
        discoveryJob = viewModelScope.launch {
            lanScanner.discoverSmbDevices().collect { state ->
                _uiState.value = _uiState.value.copy(discoveryState = state)
            }
        }
    }

    fun cancelDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _uiState.value = _uiState.value.copy(showDiscoveryDialog = false, discoveryState = null)
    }

    fun onDeviceSelected(device: DiscoveredDevice) {
        _uiState.value = _uiState.value.copy(
            pcName = device.hostName ?: "",
            ipAddress = device.ipAddress,
            showDiscoveryDialog = false,
            discoveryState = null,
            error = null
        )
        discoveryJob?.cancel()
        discoveryJob = null
    }
}
