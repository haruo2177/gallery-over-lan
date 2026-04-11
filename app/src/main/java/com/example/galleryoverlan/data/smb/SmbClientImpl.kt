package com.example.galleryoverlan.data.smb

import com.example.galleryoverlan.core.logging.AppLogger
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SmbError
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ImpersonationLevel
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.PipeShare
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    override val isHostConnected: Boolean
        get() = session != null && connection?.isConnected == true

    override suspend fun connectHost(
        host: String,
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

            smbClient = client
            connection = conn
            session = sess
            diskShare = null

            AppLogger.i("Connected to host $host (session only)", TAG)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("Host connection failed: ${e.message}", e, TAG)
            disconnect()
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun connectShare(shareName: String): AppResult<Unit> {
        val sess = session ?: return AppResult.Error(SmbError.NetworkUnreachable())
        return try {
            diskShare?.close()
            diskShare = null
            val share = sess.connectShare(shareName) as DiskShare
            diskShare = share
            AppLogger.i("Connected to share $shareName", TAG)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("Share connection failed: ${e.message}", e, TAG)
            AppResult.Error(mapException(e))
        }
    }

    override suspend fun listSharesFromSession(): AppResult<List<String>> {
        val sess = session ?: return AppResult.Error(SmbError.NetworkUnreachable())
        var pipeShare: PipeShare? = null
        return try {
            pipeShare = sess.connectShare("IPC$") as PipeShare

            val pipe = pipeShare.open(
                "srvsvc",
                SMB2ImpersonationLevel.Impersonation,
                EnumSet.of(AccessMask.GENERIC_READ, AccessMask.GENERIC_WRITE),
                EnumSet.noneOf(FileAttributes::class.java),
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
            )

            try {
                pipe.transact(RPC_BIND_SRVSVC)
                val response = pipe.transact(buildNetShareEnumAllRequest())
                val shares = parseNetShareEnumResponse(response)
                    .filter { !it.endsWith("$") }
                    .sorted()
                AppResult.Success(shares)
            } finally {
                try { pipe.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            AppLogger.e("listSharesFromSession failed: ${e.message}", e, TAG)
            AppResult.Error(mapException(e))
        } finally {
            try { pipeShare?.close() } catch (_: Exception) {}
        }
    }

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

    override suspend fun listShares(
        host: String,
        userName: String,
        password: String,
        domain: String
    ): AppResult<List<String>> {
        var client: SMBClient? = null
        var conn: Connection? = null
        var sess: Session? = null
        var pipeShare: PipeShare? = null
        return try {
            client = createClient()
            conn = client.connect(host)
            val auth = AuthenticationContext(userName, password.toCharArray(), domain)
            sess = conn.authenticate(auth)
            pipeShare = sess.connectShare("IPC$") as PipeShare

            val pipe = pipeShare.open(
                "srvsvc",
                SMB2ImpersonationLevel.Impersonation,
                EnumSet.of(AccessMask.GENERIC_READ, AccessMask.GENERIC_WRITE),
                EnumSet.noneOf(FileAttributes::class.java),
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
            )

            try {
                // DCE/RPC Bind to SRVSVC (use transact for reliable request-response)
                val bindAck = pipe.transact(RPC_BIND_SRVSVC)

                // DCE/RPC NetShareEnumAll request
                val response = pipe.transact(buildNetShareEnumAllRequest())

                val shares = parseNetShareEnumResponse(response)
                    .filter { !it.endsWith("$") }
                    .sorted()

                AppResult.Success(shares)
            } finally {
                try { pipe.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            AppLogger.e("listShares failed: ${e.message}", e, TAG)
            AppResult.Error(mapException(e))
        } finally {
            try { pipeShare?.close() } catch (_: Exception) {}
            try { sess?.close() } catch (_: Exception) {}
            try { conn?.close() } catch (_: Exception) {}
            try { client?.close() } catch (_: Exception) {}
        }
    }

    // Pre-built DCE/RPC Bind PDU for SRVSVC
    // SRVSVC UUID: {4b324fc8-1670-01d3-1278-5a47bf6ee188} v3.0
    // NDR transfer syntax: {8a885d04-1ceb-11c9-9fe8-08002b104860} v2.0
    @Suppress("MagicNumber")
    private val RPC_BIND_SRVSVC = byteArrayOf(
        // RPC header
        0x05, 0x00,       // version 5.0
        0x0B,             // packet type: Bind
        0x03,             // flags: first + last
        0x10, 0x00, 0x00, 0x00, // data representation (little-endian)
        0x48, 0x00,       // frag length: 72
        0x00, 0x00,       // auth length: 0
        0x01, 0x00, 0x00, 0x00, // call ID: 1
        // Bind fields
        0xB8.toByte(), 0x10, // max xmit frag: 4280
        0xB8.toByte(), 0x10, // max recv frag: 4280
        0x00, 0x00, 0x00, 0x00, // assoc group: 0
        0x01, 0x00, 0x00, 0x00, // num context items: 1
        // Context item 0
        0x00, 0x00,       // context ID: 0
        0x01, 0x00,       // num trans items: 1
        // Abstract syntax: SRVSVC UUID
        0xC8.toByte(), 0x4F, 0x32, 0x4B, // time_low
        0x70, 0x16,       // time_mid
        0xD3.toByte(), 0x01, // time_hi_and_version
        0x12, 0x78,       // clock_seq
        0x5A, 0x47, 0xBF.toByte(), 0x6E, 0xE1.toByte(), 0x88.toByte(), // node
        0x03, 0x00,       // version major: 3
        0x00, 0x00,       // version minor: 0
        // Transfer syntax: NDR UUID
        0x04, 0x5D, 0x88.toByte(), 0x8A.toByte(), // time_low
        0xEB.toByte(), 0x1C, // time_mid
        0xC9.toByte(), 0x11, // time_hi_and_version
        0x9F.toByte(), 0xE8.toByte(), // clock_seq
        0x08, 0x00, 0x2B, 0x10, 0x48, 0x60, // node
        0x02, 0x00,       // version: 2
        0x00, 0x00
    )

    // DCE/RPC Request PDU for NetShareEnumAll (opnum 15)
    // Uses a minimal server name "\\" to avoid hostname issues
    private fun buildNetShareEnumAllRequest(): ByteArray {
        // Stub: server name "\\<null>" as NDR pointer + conformant varying string
        //   + InfoLevel=1 + ShareEnumStruct + PrefMaxLen + ResumeHandle
        @Suppress("MagicNumber")
        val stub = byteArrayOf(
            // Server name pointer (referent ID)
            0x00, 0x00, 0x02, 0x00,
            // Max count: 2 (chars including null)
            0x02, 0x00, 0x00, 0x00,
            // Offset: 0
            0x00, 0x00, 0x00, 0x00,
            // Actual count: 2
            0x02, 0x00, 0x00, 0x00,
            // "\\" in UTF-16LE + null terminator
            0x5C, 0x00, 0x00, 0x00,
            // Info level: 1
            0x01, 0x00, 0x00, 0x00,
            // SHARE_ENUM_STRUCT: level=1
            0x01, 0x00, 0x00, 0x00,
            // Ctr pointer (referent ID)
            0x04, 0x00, 0x02, 0x00,
            // EntriesRead: 0
            0x00, 0x00, 0x00, 0x00,
            // Buffer pointer: NULL
            0x00, 0x00, 0x00, 0x00,
            // PrefMaxLen: 0xFFFFFFFF
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            // Resume handle pointer: NULL (no pointer)
            0x00, 0x00, 0x00, 0x00
        )

        val totalLen = 24 + stub.size
        val pdu = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        pdu.put(5)    // version major
        pdu.put(0)    // version minor
        pdu.put(0)    // packet type: Request
        pdu.put(3)    // flags: first + last fragment
        pdu.putInt(0x00000010) // data representation
        pdu.putShort(totalLen.toShort()) // frag length
        pdu.putShort(0)   // auth length
        pdu.putInt(1)     // call ID
        pdu.putInt(stub.size) // alloc hint
        pdu.putShort(0)   // context ID
        pdu.putShort(15)  // opnum: NetShareEnumAll
        pdu.put(stub)
        return pdu.array()
    }

    // Parse NetShareEnumAll response - extract share names from NDR-encoded data
    private fun parseNetShareEnumResponse(data: ByteArray): List<String> {
        val shares = mutableListOf<String>()
        try {
            val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            // Skip RPC response header (24 bytes)
            if (data.size < 24) return shares
            buf.position(24)

            // InfoLevel(4) + SwitchValue(4) + ReferentID(4) + EntriesRead(4) + ArrayReferentID(4)
            if (buf.remaining() < 20) return shares
            buf.getInt() // info level
            buf.getInt() // switch value
            buf.getInt() // referent ID
            val entriesRead = buf.getInt()
            buf.getInt() // array referent ID

            if (entriesRead <= 0 || entriesRead > 1000) return shares

            // Conformant array max count
            if (buf.remaining() < 4) return shares
            buf.getInt() // max count

            // Skip SHARE_INFO_1 entries: name_ptr(4) + type(4) + comment_ptr(4) = 12 bytes each
            if (buf.remaining() < entriesRead * 12) return shares
            for (i in 0 until entriesRead) {
                buf.getInt() // name pointer
                buf.getInt() // type
                buf.getInt() // comment pointer
            }

            // Read share names (conformant varying strings), skip comments
            for (i in 0 until entriesRead) {
                val name = readNdrString(buf) ?: break
                shares.add(name)
                // Skip the comment string for this entry
                skipNdrString(buf)
            }
        } catch (e: Exception) {
            AppLogger.e("parseNetShareEnumResponse failed: ${e.message}", e, TAG)
        }
        return shares
    }

    private fun readNdrString(buf: ByteBuffer): String? {
        if (buf.remaining() < 12) return null
        buf.getInt() // max count
        buf.getInt() // offset
        val actualCount = buf.getInt()
        if (actualCount <= 0 || buf.remaining() < actualCount * 2) return null
        val bytes = ByteArray(actualCount * 2)
        buf.get(bytes)
        // Align to 4 bytes
        while (buf.position() % 4 != 0 && buf.remaining() > 0) buf.get()
        return String(bytes, Charsets.UTF_16LE).trimEnd('\u0000')
    }

    private fun skipNdrString(buf: ByteBuffer) {
        if (buf.remaining() < 12) return
        buf.getInt() // max count
        buf.getInt() // offset
        val actualCount = buf.getInt()
        if (actualCount <= 0 || buf.remaining() < actualCount * 2) return
        buf.position(buf.position() + actualCount * 2)
        while (buf.position() % 4 != 0 && buf.remaining() > 0) buf.get()
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
        val fullChain = buildExceptionChain(e).lowercase()
        AppLogger.w(
            "mapException: ${e::class.simpleName}: ${e.message}",
            throwable = e,
            tag = TAG
        )
        return when {
            message.contains("authentication") || message.contains("logon") ||
                message.contains("password") || message.contains("STATUS_LOGON_FAILURE") ->
                SmbError.AuthenticationFailed(e)

            message.contains("access") && message.contains("denied") ->
                SmbError.PermissionDenied(e)

            message.contains("not found") || message.contains("STATUS_OBJECT_NAME_NOT_FOUND") ||
                message.contains("STATUS_OBJECT_PATH_NOT_FOUND") ->
                SmbError.PathNotFound("", e)

            message.contains("STATUS_BAD_NETWORK_NAME") ->
                SmbError.ShareNotFound("", e)

            // セッション期限切れ（アイドル後に頻発）
            message.contains("session_expired") || message.contains("network_session_expired") ||
                message.contains("STATUS_USER_SESSION_DELETED") ||
                message.contains("session was closed") ->
                SmbError.SessionExpired(e)

            message.contains("timeout") || message.contains("timed out") ->
                SmbError.Timeout(e)

            // 接続切断系（ソケットレベル）
            fullChain.contains("connection reset") || fullChain.contains("broken pipe") ||
                fullChain.contains("end of stream") || fullChain.contains("eof") ||
                fullChain.contains("socket closed") || fullChain.contains("stream closed") ->
                SmbError.SessionExpired(e)

            // TransportException（SMBJ固有、接続が切れた時に発生）
            e::class.simpleName == "TransportException" ||
                fullChain.contains("transport") ->
                SmbError.SessionExpired(e)

            message.contains("unreachable") || message.contains("connect") ||
                message.contains("refused") || message.contains("no route") ->
                SmbError.NetworkUnreachable(e)

            else -> SmbError.Unknown(e)
        }
    }

    /** 例外チェーン全体のメッセージを結合（原因の特定に利用） */
    private fun buildExceptionChain(e: Throwable): String {
        val sb = StringBuilder()
        var current: Throwable? = e
        while (current != null) {
            sb.append(current::class.simpleName).append(": ").append(current.message).append(" -> ")
            current = current.cause
        }
        return sb.toString()
    }
}
