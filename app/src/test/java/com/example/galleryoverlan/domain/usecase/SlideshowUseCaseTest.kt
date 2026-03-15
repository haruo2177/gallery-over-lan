package com.example.galleryoverlan.domain.usecase

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SlideshowUseCaseTest {

    private lateinit var useCase: SlideshowUseCase

    @Before
    fun setup() {
        useCase = SlideshowUseCase()
    }

    @Test
    fun `emits sequential indices wrapping around`() = runTest {
        val indices = useCase.start(
            totalImages = 3,
            startIndex = 0,
            intervalMs = 100
        ).take(5).toList()

        assertEquals(listOf(1, 2, 0, 1, 2), indices)
    }

    @Test
    fun `starts from given index`() = runTest {
        val indices = useCase.start(
            totalImages = 4,
            startIndex = 2,
            intervalMs = 100
        ).take(3).toList()

        assertEquals(listOf(3, 0, 1), indices)
    }

    @Test
    fun `single image wraps to same index`() = runTest {
        val indices = useCase.start(
            totalImages = 1,
            startIndex = 0,
            intervalMs = 100
        ).take(3).toList()

        assertEquals(listOf(0, 0, 0), indices)
    }
}
