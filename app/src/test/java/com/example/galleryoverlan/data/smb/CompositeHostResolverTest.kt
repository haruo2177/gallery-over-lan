package com.example.galleryoverlan.data.smb

import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class CompositeHostResolverTest {

    private lateinit var defaultResolver: DefaultHostResolver
    private lateinit var netBiosResolver: NetBiosNameResolver
    private lateinit var mdnsResolver: MdnsNameResolver
    private lateinit var compositeResolver: CompositeHostResolver

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        defaultResolver = mockk()
        netBiosResolver = mockk()
        mdnsResolver = mockk()
        compositeResolver = CompositeHostResolver(defaultResolver, netBiosResolver, mdnsResolver)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
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

    @Test
    fun `falls back to mDNS when DNS fails`() = runTest {
        coEvery { defaultResolver.resolve("MacBook") } throws UnknownHostException("not found")
        coEvery { mdnsResolver.resolveNameToIp("MacBook") } returns "192.168.1.30"

        val result = compositeResolver.resolve("MacBook")
        assertEquals("192.168.1.30", result)
    }

    @Test
    fun `falls back to NetBIOS when DNS and mDNS fail`() = runTest {
        coEvery { defaultResolver.resolve("DESKTOP-ABC") } throws UnknownHostException("not found")
        coEvery { mdnsResolver.resolveNameToIp("DESKTOP-ABC") } returns null
        coEvery { netBiosResolver.resolveNameToIp("DESKTOP-ABC") } returns "192.168.1.50"

        val result = compositeResolver.resolve("DESKTOP-ABC")
        assertEquals("192.168.1.50", result)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when all resolution methods fail`() = runTest {
        coEvery { defaultResolver.resolve("UNKNOWN-PC") } throws UnknownHostException("not found")
        coEvery { mdnsResolver.resolveNameToIp("UNKNOWN-PC") } returns null
        coEvery { netBiosResolver.resolveNameToIp("UNKNOWN-PC") } returns null

        compositeResolver.resolve("UNKNOWN-PC")
    }
}
