package com.example.galleryoverlan.data.smb

interface HostResolver {
    suspend fun resolve(hostName: String): String
}
