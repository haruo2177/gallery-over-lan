package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.domain.model.SlideshowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SlideshowUseCase @Inject constructor() {

    fun start(
        totalImages: Int,
        startIndex: Int,
        intervalMs: Long
    ): Flow<Int> = flow {
        var current = startIndex
        while (true) {
            delay(intervalMs)
            current = (current + 1) % totalImages
            emit(current)
        }
    }
}
