package com.example.galleryoverlan.ui.viewer

import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SlideshowState

data class ViewerUiState(
    val images: List<ImageItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showControls: Boolean = true,
    val slideshowState: SlideshowState = SlideshowState.Idle,
    val slideshowIntervalMs: Long = 1500L,
    val showIntervalPicker: Boolean = false,
    val showSlideshowEndDialog: Boolean = false
) {
    val currentImage: ImageItem?
        get() = images.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = currentIndex < images.lastIndex

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val positionText: String
        get() = if (images.isNotEmpty()) "${currentIndex + 1} / ${images.size}" else ""

    val isPlaying: Boolean
        get() = slideshowState is SlideshowState.Playing
}
