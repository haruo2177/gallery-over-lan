package com.example.galleryoverlan.core.network

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>
}
