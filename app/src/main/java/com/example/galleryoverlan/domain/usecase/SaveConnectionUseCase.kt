package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.data.security.CredentialRepository
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.domain.model.ConnectionConfig
import javax.inject.Inject

class SaveConnectionUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val credentialRepository: CredentialRepository
) {
    suspend operator fun invoke(
        hostName: String,
        shareName: String,
        userName: String,
        password: String,
        baseFolderPath: String
    ) {
        val config = ConnectionConfig(
            hostName = hostName,
            shareName = shareName,
            userName = userName,
            baseFolderPath = baseFolderPath
        )
        settingsRepository.saveConnectionConfig(config)
        credentialRepository.saveCredentials(userName, password)
    }
}
