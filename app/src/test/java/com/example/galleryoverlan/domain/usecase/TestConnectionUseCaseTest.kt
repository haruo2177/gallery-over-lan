package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TestConnectionUseCaseTest {

    private lateinit var smbRepository: SmbRepository
    private lateinit var useCase: TestConnectionUseCase

    @Before
    fun setup() {
        smbRepository = mockk()
        useCase = TestConnectionUseCase(smbRepository)
    }

    @Test
    fun `returns error when host is blank`() = runTest {
        val result = useCase("", "share", "user", "pass")
        assertTrue(result.isError)
    }

    @Test
    fun `returns error when shareName is blank`() = runTest {
        val result = useCase("host", "", "user", "pass")
        assertTrue(result.isError)
    }

    @Test
    fun `returns error when userName is blank`() = runTest {
        val result = useCase("host", "share", "", "pass")
        assertTrue(result.isError)
    }

    @Test
    fun `returns error when password is blank`() = runTest {
        val result = useCase("host", "share", "user", "")
        assertTrue(result.isError)
    }

    @Test
    fun `delegates to repository on valid input`() = runTest {
        coEvery {
            smbRepository.testConnection("host", "share", "user", "pass")
        } returns AppResult.Success(Unit)

        val result = useCase("host", "share", "user", "pass")

        assertTrue(result.isSuccess)
        coVerify { smbRepository.testConnection("host", "share", "user", "pass") }
    }

    @Test
    fun `propagates repository error`() = runTest {
        coEvery {
            smbRepository.testConnection("host", "share", "user", "pass")
        } returns AppResult.Error(RuntimeException("connection refused"))

        val result = useCase("host", "share", "user", "pass")

        assertTrue(result.isError)
        assertEquals("connection refused", (result as AppResult.Error).message)
    }
}
