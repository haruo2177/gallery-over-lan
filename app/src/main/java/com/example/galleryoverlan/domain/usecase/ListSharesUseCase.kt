package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import javax.inject.Inject

class ListSharesUseCase @Inject constructor(
    private val smbRepository: SmbRepository
) {
    suspend operator fun invoke(): AppResult<List<String>> {
        return smbRepository.listShares()
    }
}
