package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import javax.inject.Inject

class ConnectToShareUseCase @Inject constructor(
    private val smbRepository: SmbRepository
) {
    suspend operator fun invoke(shareName: String): AppResult<Unit> {
        return smbRepository.connectToShare(shareName)
    }
}
