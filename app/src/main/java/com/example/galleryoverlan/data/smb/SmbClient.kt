package com.example.galleryoverlan.data.smb

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import java.io.InputStream

interface SmbClient {
    suspend fun testConnection(
        host: String,
        shareName: String,
        userName: String,
        password: String,
        domain: String = ""
    ): AppResult<Unit>

    suspend fun connect(
        host: String,
        shareName: String,
        userName: String,
        password: String,
        domain: String = ""
    ): AppResult<Unit>

    suspend fun listFolders(path: String): AppResult<List<FolderItem>>

    suspend fun listImages(path: String): AppResult<List<ImageItem>>

    suspend fun readImageStream(path: String): AppResult<InputStream>

    suspend fun disconnect()

    val isConnected: Boolean
}
