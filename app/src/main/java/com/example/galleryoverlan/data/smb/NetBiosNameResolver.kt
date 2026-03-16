package com.example.galleryoverlan.data.smb

import android.content.Context
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves NetBIOS names to IP addresses using broadcast queries,
 * and resolves IP addresses to NetBIOS names using node status queries.
 */
@Singleton
class NetBiosNameResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val NETBIOS_NAME_PORT = 137
        private const val TIMEOUT_MS = 2000
    }

    /**
     * Resolve a NetBIOS name to an IP address via broadcast Name Query.
     */
    fun resolveNameToIp(netBiosName: String): String? {
        val broadcastAddress = getBroadcastAddress() ?: return null
        val query = buildNameQuery(netBiosName.uppercase())

        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.broadcast = true
                val sendPacket = DatagramPacket(
                    query, query.size,
                    InetAddress.getByName(broadcastAddress), NETBIOS_NAME_PORT
                )
                socket.send(sendPacket)

                val buf = ByteArray(1024)
                val recvPacket = DatagramPacket(buf, buf.size)
                socket.receive(recvPacket)

                parseNameQueryResponse(buf)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolve an IP address to a NetBIOS name via Node Status query.
     */
    fun resolveIpToName(ip: String): String? {
        val query = buildNodeStatusQuery()
        val address = try {
            InetAddress.getByName(ip)
        } catch (_: Exception) {
            return null
        }

        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                val sendPacket = DatagramPacket(query, query.size, address, NETBIOS_NAME_PORT)
                socket.send(sendPacket)

                val buf = ByteArray(1024)
                val recvPacket = DatagramPacket(buf, buf.size)
                socket.receive(recvPacket)

                parseNodeStatusResponse(buf)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getBroadcastAddress(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val linkProperties = cm.getLinkProperties(network) ?: return null

        val linkAddress = linkProperties.linkAddresses
            .firstOrNull { it.address is Inet4Address }
            ?: return null

        val deviceIp = linkAddress.address.hostAddress ?: return null
        val prefixLength = linkAddress.prefixLength

        val parts = deviceIp.split(".").map { it.toInt() }
        val ipInt = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val mask = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
        val broadcast = ipInt or mask.inv()

        return "${(broadcast shr 24) and 0xFF}.${(broadcast shr 16) and 0xFF}" +
            ".${(broadcast shr 8) and 0xFF}.${broadcast and 0xFF}"
    }

    /**
     * Build a NetBIOS Name Query request to resolve a name to IP.
     */
    private fun buildNameQuery(name: String): ByteArray {
        val packet = ByteArray(50)
        // Transaction ID
        packet[0] = 0x01; packet[1] = 0x01
        // Flags: 0x0110 = broadcast, recursion desired
        packet[2] = 0x01; packet[3] = 0x10
        // Questions: 1
        packet[4] = 0x00; packet[5] = 0x01
        // Name label length = 32
        packet[12] = 0x20
        // Encode name (padded to 15 chars with spaces, suffix 0x00)
        val padded = name.padEnd(15, ' ') + "\u0000"
        for (i in 0 until 16) {
            val ch = padded[i].code
            packet[13 + i * 2] = ('A' + ((ch shr 4) and 0x0F)).code.toByte()
            packet[14 + i * 2] = ('A' + (ch and 0x0F)).code.toByte()
        }
        packet[45] = 0x00 // name terminator
        // Type: NB (0x0020)
        packet[46] = 0x00; packet[47] = 0x20
        // Class: IN (0x0001)
        packet[48] = 0x00; packet[49] = 0x01
        return packet
    }

    /**
     * Parse a NetBIOS Name Query response to extract the IP address.
     */
    private fun parseNameQueryResponse(data: ByteArray): String? {
        if (data.size < 12) return null
        // Check answer count > 0
        val anCount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (anCount == 0) return null

        // Skip header (12 bytes) + question section
        var offset = 12
        // Skip question name
        while (offset < data.size) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) { offset++; break }
            offset += len + 1
        }
        offset += 4 // Type + Class

        // Answer section: skip name (could be pointer 0xC0xx)
        if (offset + 2 > data.size) return null
        if ((data[offset].toInt() and 0xC0) == 0xC0) {
            offset += 2
        } else {
            while (offset < data.size) {
                val len = data[offset].toInt() and 0xFF
                if (len == 0) { offset++; break }
                offset += len + 1
            }
        }

        // Type(2) + Class(2) + TTL(4) + RDLENGTH(2)
        if (offset + 10 > data.size) return null
        val rdLength = ((data[offset + 8].toInt() and 0xFF) shl 8) or (data[offset + 9].toInt() and 0xFF)
        offset += 10

        // RDATA: flags(2) + IP(4) per entry
        if (rdLength >= 6 && offset + 6 <= data.size) {
            offset += 2 // skip flags
            return "${data[offset].toInt() and 0xFF}.${data[offset + 1].toInt() and 0xFF}" +
                ".${data[offset + 2].toInt() and 0xFF}.${data[offset + 3].toInt() and 0xFF}"
        }
        return null
    }

    /**
     * Build a NetBIOS Node Status (NBSTAT) query for wildcard name "*".
     */
    private fun buildNodeStatusQuery(): ByteArray {
        val packet = ByteArray(50)
        packet[0] = 0x01; packet[1] = 0x00
        packet[4] = 0x00; packet[5] = 0x01
        packet[12] = 0x20
        val encodedName = "CKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        for (i in encodedName.indices) {
            packet[13 + i] = encodedName[i].code.toByte()
        }
        packet[45] = 0x00
        packet[46] = 0x00; packet[47] = 0x21
        packet[48] = 0x00; packet[49] = 0x01
        return packet
    }

    /**
     * Parse a NetBIOS Node Status response to extract the computer name.
     */
    private fun parseNodeStatusResponse(data: ByteArray): String? {
        if (data.size < 12) return null

        val qdCount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val anCount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (anCount == 0) return null

        var offset = 12

        for (q in 0 until qdCount) {
            while (offset < data.size) {
                val len = data[offset].toInt() and 0xFF
                if (len == 0) { offset++; break }
                offset += len + 1
            }
            offset += 4
        }

        if (offset + 2 > data.size) return null
        if ((data[offset].toInt() and 0xC0) == 0xC0) {
            offset += 2
        } else {
            while (offset < data.size) {
                val len = data[offset].toInt() and 0xFF
                if (len == 0) { offset++; break }
                offset += len + 1
            }
        }

        if (offset + 10 > data.size) return null
        offset += 10

        if (offset >= data.size) return null
        val numNames = data[offset].toInt() and 0xFF
        offset++

        for (i in 0 until numNames) {
            if (offset + 18 > data.size) return null
            val suffix = data[offset + 15].toInt() and 0xFF
            val flags = ((data[offset + 16].toInt() and 0xFF) shl 8) or (data[offset + 17].toInt() and 0xFF)
            val isGroup = (flags and 0x8000) != 0

            if (suffix == 0x00 && !isGroup) {
                val name = String(data, offset, 15, Charsets.US_ASCII).trim()
                if (name.isNotEmpty()) return name
            }
            offset += 18
        }
        return null
    }
}
