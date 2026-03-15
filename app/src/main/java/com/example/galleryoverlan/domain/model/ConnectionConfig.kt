package com.example.galleryoverlan.domain.model

data class ConnectionConfig(
    val hostName: String,
    val shareName: String,
    val userName: String,
    val baseFolderPath: String,
    val lastSuccessfulIp: String? = null
)
