package com.example.galleryoverlan.domain.model

data class DiscoveredDevice(
    val ipAddress: String,
    val hostName: String?
) {
    val displayName: String get() = hostName ?: ipAddress
}
