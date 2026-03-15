package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.ImageItem
import javax.inject.Inject

class ListImagesUseCase @Inject constructor(
    private val smbRepository: SmbRepository
) {
    suspend operator fun invoke(folderPath: String): AppResult<List<ImageItem>> {
        return smbRepository.listImages(folderPath)
    }
}
