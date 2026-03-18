package com.example.galleryoverlan.data.smb

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Resolves hostnames using mDNS (Multicast DNS) queries.
 * Supports macOS (.local) and Linux (Avahi/systemd-resolved) devices.
 */
@Singleton
class MdnsNameResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MDNS_PORT = 5353
        private const val MDNS_ADDRESS = "224.0.0.251"
        private const val TIMEOUT_MS = 2000L
    }

    /**
     * Resolve an IP address to a hostname via mDNS reverse lookup (PTR query).
     * e.g., 192.168.1.10 -> "MacBook-Pro.local"
     */
    suspend fun resolveIpToName(ip: String): String? {
        return withTimeoutOrNull(TIMEOUT_MS) {
            try {
                performReverseLookup(ip)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Resolve an mDNS hostname (e.g., "MacBook.local") to an IP address.
     */
    suspend fun resolveNameToIp(hostName: String): String? {
        val name = if (hostName.endsWith(".local")) hostName else "$hostName.local"
        return withTimeoutOrNull(TIMEOUT_MS) {
            try {
                performForwardLookup(name)
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun performReverseLookup(ip: String): String? {
        val parts = ip.split(".")
        if (parts.size != 4) return null
        // Build reverse DNS name: 10.1.168.192.in-addr.arpa
        val ptrName = "${parts[3]}.${parts[2]}.${parts[1]}.${parts[0]}.in-addr.arpa"
        val query = buildMdnsQuery(ptrName, type = 12) // PTR record

        val response = sendMdnsQuery(query) ?: return null
        return parsePtrResponse(response)
    }

    private suspend fun performForwardLookup(hostName: String): String? {
        val query = buildMdnsQuery(hostName, type = 1) // A record
        val response = sendMdnsQuery(query) ?: return null
        return parseAResponse(response)
    }

    private suspend fun sendMdnsQuery(query: ByteArray): ByteArray? {
        return suspendCancellableCoroutine { cont ->
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(0))
                    soTimeout = TIMEOUT_MS.toInt()
                }

                val mdnsGroup = InetAddress.getByName(MDNS_ADDRESS)
                val sendPacket = DatagramPacket(query, query.size, mdnsGroup, MDNS_PORT)
                socket.send(sendPacket)

                val buf = ByteArray(1500)
                val recvPacket = DatagramPacket(buf, buf.size)
                socket.receive(recvPacket)

                cont.resume(buf.copyOf(recvPacket.length))
            } catch (_: Exception) {
                cont.resume(null)
            } finally {
                socket?.close()
            }

            cont.invokeOnCancellation {
                socket?.close()
            }
        }
    }

    /**
     * Build an mDNS query packet.
     * @param name The domain name to query
     * @param type DNS record type (1=A, 12=PTR)
     */
    private fun buildMdnsQuery(name: String, type: Int): ByteArray {
        val nameParts = name.split(".")
        // Calculate name wire format length: each part has length prefix + content, plus trailing 0
        val nameLength = nameParts.sumOf { it.length + 1 } + 1

        val packet = ByteArray(12 + nameLength + 4)

        // Header
        // Transaction ID: 0x0000 (mDNS uses 0)
        packet[0] = 0x00; packet[1] = 0x00
        // Flags: 0x0000 (standard query)
        packet[2] = 0x00; packet[3] = 0x00
        // Questions: 1
        packet[4] = 0x00; packet[5] = 0x01
        // Answer/Authority/Additional: 0
        // (already 0)

        // Question section: encode domain name
        var offset = 12
        for (part in nameParts) {
            packet[offset++] = part.length.toByte()
            for (ch in part) {
                packet[offset++] = ch.code.toByte()
            }
        }
        packet[offset++] = 0x00 // name terminator

        // Type
        packet[offset++] = ((type shr 8) and 0xFF).toByte()
        packet[offset++] = (type and 0xFF).toByte()
        // Class: IN (0x0001) with unicast-response bit set (0x8001)
        packet[offset++] = 0x80.toByte()
        packet[offset++] = 0x01

        return packet
    }

    /**
     * Parse a PTR response to extract the hostname.
     */
    private fun parsePtrResponse(data: ByteArray): String? {
        if (data.size < 12) return null

        val anCount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (anCount == 0) return null

        // Skip header + question section
        var offset = 12
        val qdCount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        for (q in 0 until qdCount) {
            offset = skipDnsName(data, offset)
            offset += 4 // Type + Class
        }

        // Parse answer(s)
        for (a in 0 until anCount) {
            offset = skipDnsName(data, offset)
            if (offset + 10 > data.size) return null

            val rType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val rdLength = ((data[offset + 8].toInt() and 0xFF) shl 8) or (data[offset + 9].toInt() and 0xFF)
            offset += 10

            if (rType == 12) { // PTR
                val name = readDnsName(data, offset)
                if (name != null) {
                    // Remove trailing dot and ".local" suffix for display
                    return name.removeSuffix(".").let { fullName ->
                        if (fullName.endsWith(".local")) fullName.removeSuffix(".local") else fullName
                    }
                }
            }
            offset += rdLength
        }
        return null
    }

    /**
     * Parse an A record response to extract the IP address.
     */
    private fun parseAResponse(data: ByteArray): String? {
        if (data.size < 12) return null

        val anCount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (anCount == 0) return null

        var offset = 12
        val qdCount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        for (q in 0 until qdCount) {
            offset = skipDnsName(data, offset)
            offset += 4
        }

        for (a in 0 until anCount) {
            offset = skipDnsName(data, offset)
            if (offset + 10 > data.size) return null

            val rType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val rdLength = ((data[offset + 8].toInt() and 0xFF) shl 8) or (data[offset + 9].toInt() and 0xFF)
            offset += 10

            if (rType == 1 && rdLength == 4 && offset + 4 <= data.size) { // A record
                return "${data[offset].toInt() and 0xFF}.${data[offset + 1].toInt() and 0xFF}" +
                    ".${data[offset + 2].toInt() and 0xFF}.${data[offset + 3].toInt() and 0xFF}"
            }
            offset += rdLength
        }
        return null
    }

    /**
     * Skip a DNS name in wire format, handling compression pointers.
     */
    private fun skipDnsName(data: ByteArray, startOffset: Int): Int {
        var offset = startOffset
        while (offset < data.size) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) {
                return offset + 1
            }
            if ((len and 0xC0) == 0xC0) {
                return offset + 2 // compression pointer
            }
            offset += len + 1
        }
        return offset
    }

    /**
     * Read a DNS name from wire format, following compression pointers.
     */
    private fun readDnsName(data: ByteArray, startOffset: Int): String? {
        val parts = mutableListOf<String>()
        var offset = startOffset
        var jumps = 0

        while (offset < data.size && jumps < 10) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                // Compression pointer
                if (offset + 1 >= data.size) return null
                offset = ((len and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                jumps++
                continue
            }
            if (offset + 1 + len > data.size) return null
            parts.add(String(data, offset + 1, len, Charsets.US_ASCII))
            offset += len + 1
        }

        return if (parts.isNotEmpty()) parts.joinToString(".") else null
    }
}
