package com.example.galleryoverlan.data.network

import android.content.Context
import android.net.ConnectivityManager
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.domain.model.DiscoveredDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : LanScanner {

    companion object {
        private const val SMB_PORT = 445
        private const val CONNECT_TIMEOUT_MS = 200
        private const val NETBIOS_PORT = 137
        private const val NETBIOS_TIMEOUT_MS = 500
        private const val MAX_CONCURRENT = 50
        private const val MAX_SCAN_IPS = 1024
    }

    override fun discoverSmbDevices(): Flow<LanScanState> = channelFlow {
        val subnetInfo = getSubnetInfo()
        if (subnetInfo == null) {
            send(LanScanState.Error("WiFiに接続されていません"))
            return@channelFlow
        }

        val (deviceIp, ipRange) = subnetInfo
        val total = ipRange.size
        if (total == 0) {
            send(LanScanState.Completed(emptyList()))
            return@channelFlow
        }

        val found = ConcurrentLinkedQueue<DiscoveredDevice>()
        val checked = AtomicInteger(0)
        val semaphore = Semaphore(MAX_CONCURRENT)

        coroutineScope {
            ipRange.forEach { ip ->
                launch(dispatchers.io) {
                    semaphore.withPermit {
                        if (ip != deviceIp && isPortOpen(ip, SMB_PORT)) {
                            val hostName = resolveHostName(ip)
                            found.add(DiscoveredDevice(ip, hostName))
                        }
                        val progress = checked.incrementAndGet().toFloat() / total
                        send(LanScanState.Scanning(progress, found.toList()))
                    }
                }
            }
        }

        send(LanScanState.Completed(found.toList().sortedBy { it.ipAddress }))
    }

    private fun getSubnetInfo(): SubnetInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val linkProperties = cm.getLinkProperties(network) ?: return null

        val linkAddress = linkProperties.linkAddresses
            .firstOrNull { it.address is Inet4Address }
            ?: return null

        val deviceIp = linkAddress.address.hostAddress ?: return null
        val prefixLength = linkAddress.prefixLength
        val ipRange = calculateIpRange(deviceIp, prefixLength)

        return SubnetInfo(deviceIp, ipRange)
    }

    private fun calculateIpRange(deviceIp: String, prefixLength: Int): List<String> {
        val parts = deviceIp.split(".").map { it.toInt() }
        val ipInt = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val mask = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
        val networkAddr = ipInt and mask
        val broadcastAddr = networkAddr or mask.inv()

        val rangeStart = networkAddr + 1
        val rangeEnd = broadcastAddr - 1

        val count = (rangeEnd - rangeStart + 1).coerceAtMost(MAX_SCAN_IPS)
        return (0 until count).map { i ->
            val addr = rangeStart + i
            "${(addr shr 24) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 8) and 0xFF}.${addr and 0xFF}"
        }
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun resolveHostName(ip: String): String? {
        return withContext(dispatchers.io) {
            resolveNetBiosName(ip) ?: resolveReverseDns(ip)
        }
    }

    /**
     * NetBIOS Node Status (NBSTAT) query to get the Windows PC name.
     * Sends a UDP packet to port 137 with a wildcard name query.
     */
    private fun resolveNetBiosName(ip: String): String? {
        return try {
            // NBSTAT query for wildcard name "*"
            val query = buildNetBiosStatusQuery()
            val address = InetAddress.getByName(ip)
            val sendPacket = DatagramPacket(query, query.size, address, NETBIOS_PORT)

            DatagramSocket().use { socket ->
                socket.soTimeout = NETBIOS_TIMEOUT_MS
                socket.send(sendPacket)

                val buf = ByteArray(1024)
                val recvPacket = DatagramPacket(buf, buf.size)
                socket.receive(recvPacket)

                parseNetBiosName(buf)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildNetBiosStatusQuery(): ByteArray {
        // Transaction ID (2) + Flags (2) + Questions (2) + Answer (2) + Authority (2) + Additional (2)
        // + Encoded name (34) + Type NBSTAT (2) + Class IN (2)
        val packet = ByteArray(50)
        // Transaction ID
        packet[0] = 0x01; packet[1] = 0x00
        // Flags: 0x0000 (standard query)
        // Questions: 1
        packet[4] = 0x00; packet[5] = 0x01
        // Name: encoded wildcard "*" (0x20 length, then 32 bytes of encoded name, then 0x00)
        packet[12] = 0x20 // length = 32
        // Encode "*" + 15 spaces as NetBIOS first-level encoding
        // '*' = 0x2A -> 'C','K'  ;  ' ' = 0x20 -> 'C','A'
        val encodedName = "CKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        for (i in encodedName.indices) {
            packet[13 + i] = encodedName[i].code.toByte()
        }
        packet[45] = 0x00 // name terminator
        // Type: NBSTAT (0x0021)
        packet[46] = 0x00; packet[47] = 0x21
        // Class: IN (0x0001)
        packet[48] = 0x00; packet[49] = 0x01
        return packet
    }

    private fun parseNetBiosName(data: ByteArray): String? {
        if (data.size < 12) return null

        val qdCount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val anCount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (anCount == 0) return null

        var offset = 12

        // Skip question section (only if present)
        for (q in 0 until qdCount) {
            // Skip name labels
            while (offset < data.size) {
                val len = data[offset].toInt() and 0xFF
                if (len == 0) { offset++; break }
                offset += len + 1
            }
            offset += 4 // Type + Class
        }

        // Answer section: skip answer name (pointer 0xC0xx or label sequence)
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
        offset += 10

        // Number of names
        if (offset >= data.size) return null
        val numNames = data[offset].toInt() and 0xFF
        offset++

        // Each name entry: 15 bytes name + 1 byte suffix + 2 bytes flags = 18 bytes
        for (i in 0 until numNames) {
            if (offset + 18 > data.size) return null
            val suffix = data[offset + 15].toInt() and 0xFF
            val flags = ((data[offset + 16].toInt() and 0xFF) shl 8) or (data[offset + 17].toInt() and 0xFF)
            val isGroup = (flags and 0x8000) != 0

            // Suffix 0x00 + not a group = computer name
            if (suffix == 0x00 && !isGroup) {
                val name = String(data, offset, 15, Charsets.US_ASCII).trim()
                if (name.isNotEmpty()) return name
            }
            offset += 18
        }
        return null
    }

    private fun resolveReverseDns(ip: String): String? {
        return try {
            val addr = InetAddress.getByName(ip)
            val hostName = addr.canonicalHostName
            if (hostName != ip) hostName else null
        } catch (_: Exception) {
            null
        }
    }

    private data class SubnetInfo(
        val deviceIp: String,
        val ipRange: List<String>
    )
}
