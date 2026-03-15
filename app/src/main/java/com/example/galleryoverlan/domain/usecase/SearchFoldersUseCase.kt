package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.domain.model.FolderItem
import javax.inject.Inject

class SearchFoldersUseCase @Inject constructor() {
    operator fun invoke(folders: List<FolderItem>, query: String): List<FolderItem> {
        if (query.isBlank()) return folders
        val lowerQuery = query.lowercase()
        return folders.filter { it.name.lowercase().contains(lowerQuery) }
    }
}
