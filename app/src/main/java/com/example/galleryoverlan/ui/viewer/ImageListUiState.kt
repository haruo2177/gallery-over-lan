package com.example.galleryoverlan.ui.viewer

import com.example.galleryoverlan.domain.model.ImageItem
import com.example.galleryoverlan.domain.model.SortOrder

data class ImageListUiState(
    val images: List<ImageItem> = emptyList(),
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    val folderPath: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val showSortMenu: Boolean = false
)
