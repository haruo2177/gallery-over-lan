package com.example.galleryoverlan.data.network

import android.content.Context
import android.net.ConnectivityManager
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.data.smb.NetBiosNameResolver
import com.example.galleryoverlan.domain.model.DiscoveredDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
    private val netBiosResolver: NetBiosNameResolver
) : LanScanner {

    companion object {
        private const val SMB_PORT = 445
        private const val CONNECT_TIMEOUT_MS = 200
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
            netBiosResolver.resolveIpToName(ip) ?: resolveReverseDns(ip)
        }
    }

    private fun resolveReverseDns(ip: String): String? {
        return try {
            val addr = java.net.InetAddress.getByName(ip)
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
