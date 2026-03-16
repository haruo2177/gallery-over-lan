package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.security.CredentialRepository
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.ConnectionConfig
import javax.inject.Inject

class ConnectToHostUseCase @Inject constructor(
    private val smbRepository: SmbRepository,
    private val settingsRepository: SettingsRepository,
    private val credentialRepository: CredentialRepository
) {
    suspend operator fun invoke(
        hostName: String,
        userName: String,
        password: String
    ): AppResult<Unit> {
        val result = smbRepository.connectToHost(hostName, userName, password)
        if (result.isSuccess) {
            val config = ConnectionConfig(
                hostName = hostName,
                userName = userName
            )
            settingsRepository.saveConnectionConfig(config)
            credentialRepository.saveCredentials(userName, password)
        }
        return result
    }
}
