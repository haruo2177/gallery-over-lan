package com.example.galleryoverlan.data.smb

import com.example.galleryoverlan.core.logging.AppLogger
import javax.inject.Inject

class CompositeHostResolver @Inject constructor(
    private val defaultResolver: DefaultHostResolver,
    private val netBiosResolver: NetBiosNameResolver
) : HostResolver {

    companion object {
        private const val TAG = "HostResolver"
    }

    override suspend fun resolve(hostName: String): String {
        // If it's already an IP address, return as-is
        if (isIpAddress(hostName)) return hostName

        // Try standard DNS resolution
        try {
            return defaultResolver.resolve(hostName)
        } catch (e: Exception) {
            AppLogger.w("DNS resolution failed for $hostName, trying NetBIOS", tag = TAG)
        }

        // Fallback: NetBIOS broadcast name query
        val ip = netBiosResolver.resolveNameToIp(hostName)
        if (ip != null) {
            AppLogger.i("NetBIOS resolved $hostName -> $ip", TAG)
            return ip
        }

        throw IllegalStateException("Could not resolve host: $hostName")
    }

    private fun isIpAddress(host: String): Boolean {
        return host.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    }
}
