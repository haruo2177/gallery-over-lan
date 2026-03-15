package com.example.galleryoverlan.data.smb

import java.net.InetAddress
import javax.inject.Inject

class DefaultHostResolver @Inject constructor() : HostResolver {
    override suspend fun resolve(hostName: String): String {
        return InetAddress.getByName(hostName).hostAddress
            ?: throw IllegalStateException("Could not resolve host: $hostName")
    }
}
