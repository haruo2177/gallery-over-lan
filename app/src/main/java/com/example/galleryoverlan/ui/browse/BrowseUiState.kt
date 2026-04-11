package com.example.galleryoverlan.ui.browse

import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SortOrder

data class BrowseUiState(
    val level: BrowseLevel = BrowseLevel.Shares,
    val shares: List<String> = emptyList(),
    val currentShareName: String = "",
    val currentPath: String = "",
    val folders: List<FolderItem> = emptyList(),
    val images: List<ImageItem> = emptyList(),
    val breadcrumbs: List<BrowseBreadcrumbItem> = emptyList(),
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val showSortMenu: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val targetScrollIndex: Int = 0
)

sealed class BrowseLevel {
    data object Shares : BrowseLevel()
    data object Folder : BrowseLevel()
}

data class BrowseBreadcrumbItem(
    val name: String,
    val path: String
)
