package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.ImageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ListImagesUseCaseTest {

    private lateinit var smbRepository: SmbRepository
    private lateinit var useCase: ListImagesUseCase

    @Before
    fun setup() {
        smbRepository = mockk()
        useCase = ListImagesUseCase(smbRepository)
    }

    @Test
    fun `returns images from repository`() = runTest {
        val images = listOf(
            ImageItem("photo1.jpg", "folder/photo1.jpg", 1024, 1000L),
            ImageItem("photo2.png", "folder/photo2.png", 2048, 2000L)
        )
        coEvery { smbRepository.listImages("folder") } returns AppResult.Success(images)

        val result = useCase("folder")

        assertTrue(result.isSuccess)
        assertEquals(2, (result as AppResult.Success).data.size)
    }

    @Test
    fun `propagates error from repository`() = runTest {
        coEvery { smbRepository.listImages("bad") } returns AppResult.Error(
            RuntimeException("path not found")
        )

        val result = useCase("bad")

        assertTrue(result.isError)
    }
}
