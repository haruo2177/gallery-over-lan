package com.example.galleryoverlan.ui.viewer

import com.example.galleryoverlan.domain.model.ImageItem

data class ViewerUiState(
    val images: List<ImageItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showControls: Boolean = true
) {
    val currentImage: ImageItem?
        get() = images.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = currentIndex < images.lastIndex

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val positionText: String
        get() = if (images.isNotEmpty()) "${currentIndex + 1} / ${images.size}" else ""
}
