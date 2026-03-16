package com.example.galleryoverlan.ui.connect

import com.example.galleryoverlan.data.network.LanScanState

data class ConnectUiState(
    val pcName: String = "",
    val ipAddress: String = "",
    val userName: String = "",
    val password: String = "",
    val isConnecting: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val showDiscoveryDialog: Boolean = false,
    val discoveryState: LanScanState? = null
)
