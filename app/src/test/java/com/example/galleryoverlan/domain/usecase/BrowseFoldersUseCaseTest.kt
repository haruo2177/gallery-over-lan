package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.FolderItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrowseFoldersUseCaseTest {

    private lateinit var smbRepository: SmbRepository
    private lateinit var useCase: BrowseFoldersUseCase

    @Before
    fun setup() {
        smbRepository = mockk()
        useCase = BrowseFoldersUseCase(smbRepository)
    }

    @Test
    fun `returns folders from repository`() = runTest {
        val folders = listOf(
            FolderItem("Photos", "base/Photos"),
            FolderItem("Videos", "base/Videos")
        )
        coEvery { smbRepository.listFolders("base") } returns AppResult.Success(folders)

        val result = useCase("base")

        assertTrue(result.isSuccess)
        assertEquals(2, (result as AppResult.Success).data.size)
    }

    @Test
    fun `propagates error`() = runTest {
        coEvery { smbRepository.listFolders("bad") } returns AppResult.Error(
            RuntimeException("not found")
        )

        val result = useCase("bad")
        assertTrue(result.isError)
    }
}
