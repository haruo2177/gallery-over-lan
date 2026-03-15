package com.example.galleryoverlan.data.smb

import com.example.galleryoverlan.core.logging.AppLogger
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SmbError
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbClientImpl @Inject constructor() : SmbClient {

    companion object {
        private const val TAG = "SmbClient"
        private val IMAGE_EXTENSIONS = setOf(
            ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif"
        )
        private const val CONNECT_TIMEOUT_SEC = 10L
        private const val READ_TIMEOUT_SEC = 30L
    }

    private var smbClient: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null

    override val isConnected: Boolean
        get() = diskShare != null && connection?.isConnected == true

    override suspend fun testConnection(
        host: String,
        shareName: String,
        userName: String,
        password: String,
        domain: String
    ): AppResult<Unit> {
        return try {
            val client = createClient()
            val conn = client.connect(host)
            val auth = AuthenticationContext(userName, password.toCharArray(), domain)
            val sess = conn.authenticate(auth)
            val share = sess.connectShare(shareName) as DiskShare
            share.close()
            sess.close()
            conn.close()
            client.close()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun connect(
        host: String,
        shareName: String,
        userName: String,
        password: String,
        domain: String
    ): AppResult<Unit> {
        return try {
            disconnect()
            val client = createClient()
            val conn = client.connect(host)
            val auth = AuthenticationContext(userName, password.toCharArray(), domain)
            val sess = conn.authenticate(auth)
            val share = sess.connectShare(shareName) as DiskShare

            smbClient = client
            connection = conn
            session = sess
            diskShare = share

            AppLogger.i("Connected to $host/$shareName", TAG)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("Connection failed: ${e.message}", e, TAG)
            disconnect()
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun listFolders(path: String): AppResult<List<FolderItem>> {
        val share = diskShare ?: return AppResult.Error(
            SmbError.NetworkUnreachable()
        )
        return try {
            val listing = share.list(path)
            val folders = listing
                .filter { entry ->
                    val attrs = entry.fileAttributes
                    attrs and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L &&
                        entry.fileName != "." && entry.fileName != ".."
                }
                .map { entry ->
                    FolderItem(
                        name = entry.fileName,
                        path = if (path.isEmpty()) entry.fileName else "$path/${entry.fileName}"
                    )
                }
                .sortedBy { it.name.lowercase() }
            AppResult.Success(folders)
        } catch (e: Exception) {
            AppLogger.e("listFolders failed for path=$path", e, TAG)
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun listImages(path: String): AppResult<List<ImageItem>> {
        val share = diskShare ?: return AppResult.Error(
            SmbError.NetworkUnreachable()
        )
        return try {
            val listing = share.list(path)
            val images = listing
                .filter { entry ->
                    val attrs = entry.fileAttributes
                    attrs and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value == 0L &&
                        isImageFile(entry.fileName)
                }
                .map { entry ->
                    ImageItem(
                        name = entry.fileName,
                        path = if (path.isEmpty()) entry.fileName else "$path/${entry.fileName}",
                        sizeBytes = entry.endOfFile,
                        lastModified = entry.changeTime.toEpochMillis()
                    )
                }
                .sortedBy { it.name.lowercase() }
            AppResult.Success(images)
        } catch (e: Exception) {
            AppLogger.e("listImages failed for path=$path", e, TAG)
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun readImageStream(path: String): AppResult<InputStream> {
        val share = diskShare ?: return AppResult.Error(
            SmbError.NetworkUnreachable()
        )
        return try {
            val file = share.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            AppResult.Success(file.inputStream)
        } catch (e: Exception) {
            AppLogger.e("readImageStream failed for path=$path", e, TAG)
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun disconnect() {
        try {
            diskShare?.close()
        } catch (_: Exception) { }
        try {
            session?.close()
        } catch (_: Exception) { }
        try {
            connection?.close()
        } catch (_: Exception) { }
        try {
            smbClient?.close()
        } catch (_: Exception) { }
        diskShare = null
        session = null
        connection = null
        smbClient = null
    }

    private fun createClient(): SMBClient {
        val config = SmbConfig.builder()
            .withTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .withSoTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
        return SMBClient(config)
    }

    private fun isImageFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return IMAGE_EXTENSIONS.any { lower.endsWith(it) }
    }

    private fun mapException(e: Exception): SmbError {
        val message = e.message?.lowercase() ?: ""
        return when {
            message.contains("authentication") || message.contains("logon") ||
                message.contains("password") || message.contains("STATUS_LOGON_FAILURE") ->
                SmbError.AuthenticationFailed(e)

            message.contains("access") && message.contains("denied") ->
                SmbError.PermissionDenied(e)

            message.contains("not found") || message.contains("STATUS_OBJECT_NAME_NOT_FOUND") ||
                message.contains("STATUS_OBJECT_PATH_NOT_FOUND") ->
                SmbError.PathNotFound("", e)

            message.contains("STATUS_BAD_NETWORK_NAME") || message.contains("share") ->
                SmbError.ShareNotFound("", e)

            message.contains("timeout") || message.contains("timed out") ->
                SmbError.Timeout(e)

            message.contains("unreachable") || message.contains("connect") ||
                message.contains("refused") || message.contains("no route") ->
                SmbError.NetworkUnreachable(e)

            else -> SmbError.Unknown(e)
        }
    }
}
