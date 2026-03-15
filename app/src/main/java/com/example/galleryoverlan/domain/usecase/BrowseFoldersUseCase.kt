package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.FolderItem
import javax.inject.Inject

class BrowseFoldersUseCase @Inject constructor(
    private val smbRepository: SmbRepository
) {
    suspend operator fun invoke(path: String): AppResult<List<FolderItem>> {
        return smbRepository.listFolders(path)
    }
}
