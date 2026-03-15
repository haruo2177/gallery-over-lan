package com.example.galleryoverlan.domain.model

sealed class SlideshowState {
    data object Idle : SlideshowState()
    data class Playing(val intervalMs: Long) : SlideshowState()
    data object Paused : SlideshowState()
}
