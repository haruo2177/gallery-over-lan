package com.example.galleryoverlan.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.galleryoverlan.domain.model.SortOrder
import com.example.galleryoverlan.ui.navigation.Routes
import com.example.galleryoverlan.ui.viewer.SmbImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (folderPath: String, startIndex: Int, autoSlideshow: Boolean) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // Restore scroll position when folder contents change
    LaunchedEffect(state.currentPath, state.targetScrollIndex) {
        if (state.targetScrollIndex > 0) {
            gridState.scrollToItem(state.targetScrollIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToViewer.collect { (folderPath, index) ->
            onNavigateToViewer(folderPath, index, false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            onNavigateBack()
        }
    }

    BackHandler {
        viewModel.saveScrollPosition(gridState.firstVisibleItemIndex)
        if (!viewModel.onBackPressed()) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (state.level) {
                        is BrowseLevel.Shares -> Text("共有一覧")
                        is BrowseLevel.Folder -> Text(state.currentShareName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveScrollPosition(gridState.firstVisibleItemIndex)
                        if (!viewModel.onBackPressed()) {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    if (state.level is BrowseLevel.Folder && state.images.isNotEmpty()) {
                        IconButton(onClick = {
                            onNavigateToViewer(state.currentPath, 0, true)
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "スライドショー")
                        }
                    }
                    if (state.level is BrowseLevel.Folder) {
                        Box {
                            IconButton(onClick = viewModel::toggleSortMenu) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "ソート")
                            }
                            DropdownMenu(
                                expanded = state.showSortMenu,
                                onDismissRequest = viewModel::toggleSortMenu
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                fontWeight = if (order == state.sortOrder) {
                                                    FontWeight.Bold
                                                } else {
                                                    null
                                                }
                                            )
                                        },
                                        onClick = { viewModel.onSortOrderChange(order) }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "更新")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Breadcrumb
            if (state.level is BrowseLevel.Folder) {
                BrowseBreadcrumb(
                    items = state.breadcrumbs,
                    onItemClick = { item ->
                        viewModel.saveScrollPosition(gridState.firstVisibleItemIndex)
                        viewModel.onBreadcrumbClick(item)
                    }
                )
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    state.error != null -> {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    state.level is BrowseLevel.Shares -> {
                        SharesList(
                            shares = state.shares,
                            onShareClick = viewModel::onShareSelected
                        )
                    }

                    else -> {
                        FolderContents(
                            state = state,
                            gridState = gridState,
                            onFolderClick = { path ->
                                viewModel.saveScrollPosition(gridState.firstVisibleItemIndex)
                                viewModel.navigateTo(path)
                            },
                            onImageClick = viewModel::onImageClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseBreadcrumb(
    items: List<BrowseBreadcrumbItem>,
    onItemClick: (BrowseBreadcrumbItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val isLast = index == items.lastIndex
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLast) FontWeight.Bold else null,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = if (!isLast) {
                    Modifier.clickable { onItemClick(item) }
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
private fun SharesList(
    shares: List<String>,
    onShareClick: (String) -> Unit
) {
    if (shares.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("共有フォルダが見つかりませんでした")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize()
    ) {
        items(shares, key = { it }) { share ->
            ListItem(
                headlineContent = { Text(share) },
                leadingContent = {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onShareClick(share) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FolderContents(
    state: BrowseUiState,
    gridState: LazyGridState,
    onFolderClick: (String) -> Unit,
    onImageClick: (Int) -> Unit
) {
    if (state.folders.isEmpty() && state.images.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("このフォルダにフォルダや画像はありません")
        }
        return
    }

    val context = LocalContext.current
    val imageLoader = remember { coil.Coil.imageLoader(context) }
    val density = LocalDensity.current
    val cellSizePx = remember(density) {
        with(density) { 120.dp.toPx().roundToInt() }
    }

    // Prefetch images beyond visible area
    val prefetchRange = 20
    val lastVisibleIndex by remember {
        derivedStateOf {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
    }
    val folderCount = state.folders.size
    LaunchedEffect(lastVisibleIndex, state.images.size) {
        val lastVisibleImageIndex = lastVisibleIndex - folderCount
        val prefetchStart = (lastVisibleImageIndex + 1).coerceAtLeast(0)
        val prefetchEnd = (prefetchStart + prefetchRange).coerceAtMost(state.images.size)
        for (i in prefetchStart until prefetchEnd) {
            val request = ImageRequest.Builder(context)
                .data(SmbImageRequest(path = state.images[i].path, thumbnail = true, thumbnailSizePx = cellSizePx))
                .size(cellSizePx)
                .build()
            imageLoader.enqueue(request)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Folders as full-width list items
        items(
            items = state.folders,
            key = { "folder:${it.path}" },
            span = { GridItemSpan(maxLineSpan) }
        ) { folder ->
            Column {
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = folder.imageCount?.let {
                        { Text("${it}枚") }
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { onFolderClick(folder.path) }
                )
                HorizontalDivider()
            }
        }

        // Images as grid cells
        itemsIndexed(
            items = state.images,
            key = { _, image -> "image:${image.path}" }
        ) { index, image ->
            val model = remember(image.path, cellSizePx) {
                SmbImageRequest(path = image.path, thumbnail = true, thumbnailSizePx = cellSizePx)
            }
            AsyncImage(
                model = model,
                contentDescription = image.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onImageClick(index) }
            )
        }
    }
}
