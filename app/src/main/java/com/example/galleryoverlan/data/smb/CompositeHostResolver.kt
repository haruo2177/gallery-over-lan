package com.example.galleryoverlan.data.smb

import com.example.galleryoverlan.core.logging.AppLogger
import javax.inject.Inject

class CompositeHostResolver @Inject constructor(
    private val defaultResolver: DefaultHostResolver
) : HostResolver {

    companion object {
        private const val TAG = "HostResolver"
    }

    override suspend fun resolve(hostName: String): String {
        // If it's already an IP address, return as-is
        if (isIpAddress(hostName)) return hostName

        // Try standard DNS resolution, fall back to hostname as-is
        return try {
            defaultResolver.resolve(hostName)
        } catch (e: Exception) {
            AppLogger.w("DNS resolution failed for $hostName, using as-is", tag = TAG)
            hostName
        }
    }

    private fun isIpAddress(host: String): Boolean {
        return host.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    }
}
