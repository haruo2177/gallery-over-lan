package com.example.galleryoverlan.ui.browse

import com.example.galleryoverlan.data.search.SearchHistoryEntry
import com.example.galleryoverlan.domain.model.FolderItem
import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SearchOptions
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorDetail: String? = null,
    val imageLoadError: String? = null,
    val targetScrollIndex: Int = 0,
    // Search
    val showSearchPanel: Boolean = false,
    val searchQuery: String = "",
    val searchOptions: SearchOptions = SearchOptions(),
    val searchHistory: List<SearchHistoryEntry> = emptyList(),
    val isSearchActive: Boolean = false,
    val filteredFolders: List<FolderItem>? = null,
    val filteredImages: List<ImageItem>? = null,
    // Random
    val showRandomDialog: Boolean = false,
    val randomFolders: List<FolderItem> = emptyList(),
    val randomCount: Int = 7
) {
    val displayFolders: List<FolderItem> get() = filteredFolders ?: folders
    val displayImages: List<ImageItem> get() = filteredImages ?: images
}

sealed class BrowseLevel {
    data object Shares : BrowseLevel()
    data object Folder : BrowseLevel()
}

data class BrowseBreadcrumbItem(
    val name: String,
    val path: String
)

data class CachedSearchState(
    val query: String,
    val options: SearchOptions
)
