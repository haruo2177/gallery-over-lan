package com.example.galleryoverlan.ui.browser

import com.example.galleryoverlan.domain.model.FolderItem

data class BrowserUiState(
    val currentPath: String = "",
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val filteredFolders: List<FolderItem> = emptyList(),
    val imageCount: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null
)

data class BreadcrumbItem(
    val name: String,
    val path: String
)
