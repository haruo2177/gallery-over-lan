package com.example.galleryoverlan.data.smb

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class CompositeHostResolverTest {

    private lateinit var defaultResolver: DefaultHostResolver
    private lateinit var compositeResolver: CompositeHostResolver

    @Before
    fun setup() {
        defaultResolver = mockk()
        compositeResolver = CompositeHostResolver(defaultResolver)
    }

    @Test
    fun `IP address is returned as-is`() = runTest {
        val result = compositeResolver.resolve("192.168.1.100")
        assertEquals("192.168.1.100", result)
    }

    @Test
    fun `hostname delegates to default resolver`() = runTest {
        coEvery { defaultResolver.resolve("DESKTOP-ABC") } returns "192.168.1.50"

        val result = compositeResolver.resolve("DESKTOP-ABC")
        assertEquals("192.168.1.50", result)
    }

    @Test(expected = UnknownHostException::class)
    fun `throws when hostname resolution fails`() = runTest {
        coEvery { defaultResolver.resolve("UNKNOWN-PC") } throws UnknownHostException("not found")

        compositeResolver.resolve("UNKNOWN-PC")
    }
}
