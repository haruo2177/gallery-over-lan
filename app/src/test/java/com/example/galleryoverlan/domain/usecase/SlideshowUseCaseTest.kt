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
    fun `emits sequential indices and stops at last image`() = runTest {
        val indices = useCase.start(
            totalImages = 3,
            startIndex = 0,
            intervalMs = 100
        ).toList()

        assertEquals(listOf(1, 2), indices)
    }

    @Test
    fun `starts from given index and stops at last image`() = runTest {
        val indices = useCase.start(
            totalImages = 4,
            startIndex = 2,
            intervalMs = 100
        ).toList()

        assertEquals(listOf(3), indices)
    }

    @Test
    fun `single image emits once and stops`() = runTest {
        val indices = useCase.start(
            totalImages = 1,
            startIndex = 0,
            intervalMs = 100
        ).toList()

        assertEquals(listOf(0), indices)
    }

    @Test
    fun `wraps around before stopping at last image`() = runTest {
        val indices = useCase.start(
            totalImages = 3,
            startIndex = 1,
            intervalMs = 100
        ).toList()

        assertEquals(listOf(2), indices)
    }

    @Test
    fun `starting from last image goes through all and stops`() = runTest {
        val indices = useCase.start(
            totalImages = 3,
            startIndex = 2,
            intervalMs = 100
        ).toList()

        assertEquals(listOf(0, 1, 2), indices)
    }
}
