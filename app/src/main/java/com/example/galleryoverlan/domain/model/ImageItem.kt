package com.example.galleryoverlan.domain.model

data class ImageItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long
)
