package com.example.galleryoverlan.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `Success contains data`() {
        val result = AppResult.Success("hello")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("hello", result.getOrNull())
        assertEquals("hello", result.getOrThrow())
    }

    @Test
    fun `Error contains exception and message`() {
        val error = AppResult.Error(RuntimeException("fail"), "Something failed")
        assertFalse(error.isSuccess)
        assertTrue(error.isError)
        assertNull(error.getOrNull())
        assertEquals("Something failed", error.message)
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws on Error`() {
        val error = AppResult.Error(RuntimeException("fail"))
        error.getOrThrow()
    }

    @Test
    fun `map transforms Success data`() {
        val result = AppResult.Success(5)
        val mapped = result.map { it * 2 }
        assertEquals(10, (mapped as AppResult.Success).data)
    }

    @Test
    fun `map preserves Error`() {
        val error = AppResult.Error(RuntimeException("fail"))
        val mapped = error.map { "should not reach" }
        assertTrue(mapped.isError)
    }

    @Test
    fun `runCatchingApp captures success`() {
        val result = runCatchingApp { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatchingApp captures exception`() {
        val result = runCatchingApp { throw IllegalArgumentException("bad") }
        assertTrue(result.isError)
        assertEquals("bad", (result as AppResult.Error).message)
    }
}
