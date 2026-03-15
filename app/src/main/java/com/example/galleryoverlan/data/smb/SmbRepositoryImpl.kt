package com.example.galleryoverlan.data.smb

import com.example.galleryoverlan.core.logging.AppLogger
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.security.CredentialRepository
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SmbError
import kotlinx.coroutines.flow.first
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbRepositoryImpl @Inject constructor(
    private val smbClient: SmbClient,
    private val settingsRepository: SettingsRepository,
    private val credentialRepository: CredentialRepository,
    private val hostResolver: HostResolver
) : SmbRepository {

    companion object {
        private const val TAG = "SmbRepository"
    }

    override suspend fun testConnection(
        host: String,
        shareName: String,
        userName: String,
        password: String
    ): AppResult<Unit> {
        val resolvedHost = try {
            hostResolver.resolve(host)
        } catch (e: Exception) {
            return AppResult.Error(SmbError.NetworkUnreachable(e))
        }
        val result = smbClient.testConnection(resolvedHost, shareName, userName, password)
        if (result.isSuccess) {
            settingsRepository.updateLastSuccessfulIp(resolvedHost)
        }
        return result
    }

    override suspend fun connectWithSavedConfig(): AppResult<Unit> {
        val config = settingsRepository.connectionConfig.first()
            ?: return AppResult.Error(SmbError.Unknown(IllegalStateException("No saved configuration")))

        val credentials = credentialRepository.getCredentials()
            ?: return AppResult.Error(SmbError.AuthenticationFailed())

        val host = resolveHost(config.hostName, config.lastSuccessfulIp)
            ?: return AppResult.Error(SmbError.NetworkUnreachable())

        val result = smbClient.connect(host, config.shareName, credentials.first, credentials.second)
        if (result.isSuccess) {
            settingsRepository.updateLastSuccessfulIp(host)
        }
        return result
    }

    override suspend fun listFolders(path: String): AppResult<List<FolderItem>> {
        ensureConnected()
        return smbClient.listFolders(path)
    }

    override suspend fun listImages(path: String): AppResult<List<ImageItem>> {
        ensureConnected()
        return smbClient.listImages(path)
    }

    override suspend fun readImageStream(path: String): AppResult<InputStream> {
        ensureConnected()
        return smbClient.readImageStream(path)
    }

    override suspend fun disconnect() {
        smbClient.disconnect()
    }

    override fun isConnected(): Boolean = smbClient.isConnected

    private suspend fun ensureConnected() {
        if (!smbClient.isConnected) {
            AppLogger.i("Re-establishing connection", TAG)
            connectWithSavedConfig()
        }
    }

    private suspend fun resolveHost(hostName: String, lastSuccessfulIp: String?): String? {
        // Try cached IP first for speed
        if (lastSuccessfulIp != null) {
            return try {
                hostResolver.resolve(lastSuccessfulIp)
            } catch (_: Exception) {
                AppLogger.w("Cached IP $lastSuccessfulIp failed, trying hostname", tag = TAG)
                tryResolveHostName(hostName)
            }
        }
        return tryResolveHostName(hostName)
    }

    private suspend fun tryResolveHostName(hostName: String): String? {
        return try {
            hostResolver.resolve(hostName)
        } catch (e: Exception) {
            AppLogger.e("Host resolution failed for $hostName", e, TAG)
            null
        }
    }
}
