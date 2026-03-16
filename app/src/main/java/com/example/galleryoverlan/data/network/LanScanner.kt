package com.example.galleryoverlan.data.network

import com.example.galleryoverlan.domain.model.DiscoveredDevice
import kotlinx.coroutines.flow.Flow

interface LanScanner {
    fun discoverSmbDevices(): Flow<LanScanState>
}

sealed class LanScanState {
    data class Scanning(
        val progress: Float,
        val devicesFound: List<DiscoveredDevice>
    ) : LanScanState()

    data class Completed(
        val devices: List<DiscoveredDevice>
    ) : LanScanState()

    data class Error(
        val message: String
    ) : LanScanState()
}
