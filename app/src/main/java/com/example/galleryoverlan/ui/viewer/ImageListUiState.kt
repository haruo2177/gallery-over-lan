package com.example.galleryoverlan.ui.viewer

import com.example.galleryoverlan.domain.model.ImageItem

data class ImageListUiState(
    val images: List<ImageItem> = emptyList(),
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    val folderPath: String = ""
)
