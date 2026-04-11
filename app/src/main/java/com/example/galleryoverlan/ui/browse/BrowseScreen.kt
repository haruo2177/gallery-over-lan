package com.example.galleryoverlan.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
        if (state.showSearchPanel) {
            viewModel.toggleSearchPanel()
        } else if (!viewModel.onBackPressed()) {
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
                    if (state.level is BrowseLevel.Folder && state.displayImages.isNotEmpty()) {
                        IconButton(onClick = {
                            onNavigateToViewer(state.currentPath, 0, true)
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "スライドショー")
                        }
                    }
                    if (state.level is BrowseLevel.Folder && state.isSearchActive) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "検索解除")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.level is BrowseLevel.Folder) {
                BottomAppBar {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::toggleSearchPanel) {
                        if (state.isSearchActive) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("!") }
                                }
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = "検索")
                            }
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "検索")
                        }
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "更新")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search panel
            if (state.level is BrowseLevel.Folder) {
                SearchPanel(
                    visible = state.showSearchPanel,
                    query = state.searchQuery,
                    options = state.searchOptions,
                    history = state.searchHistory,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onOptionsChange = viewModel::onSearchOptionsChange,
                    onSearch = viewModel::executeSearch,
                    onClear = viewModel::clearSearch,
                    onDismiss = viewModel::toggleSearchPanel,
                    onHistoryItemClick = viewModel::onHistoryItemClick,
                    onHistoryItemDelete = viewModel::onHistoryItemDelete,
                    onClearHistory = viewModel::clearSearchHistory
                )
            }

            // Search active indicator
            if (state.isSearchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "検索結果: ${state.displayFolders.size + state.displayImages.size}件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

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

            // Item count summary (always visible)
            if (state.level is BrowseLevel.Folder && !state.isLoading && state.error == null) {
                val folderCount = state.displayFolders.size
                val imageCount = state.displayImages.size
                if (folderCount > 0 || imageCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val parts = buildList {
                            if (folderCount > 0) add("フォルダ: ${folderCount}件")
                            if (imageCount > 0) add("画像: ${imageCount}件")
                        }
                        Text(
                            text = parts.joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::refresh,
                                enabled = !state.isLoading,
                                modifier = Modifier.width(96.dp)
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("再試行")
                                }
                            }
                        }
                    }

                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                            onImageClick = viewModel::onImageClick,
                            onBack = {
                                viewModel.saveScrollPosition(gridState.firstVisibleItemIndex)
                                if (!viewModel.onBackPressed()) {
                                    onNavigateBack()
                                }
                            }
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
            val displayName = if (item.name.length > 10) item.name.take(10) + "..." else item.name
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLast) FontWeight.Bold else null,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = if (!isLast) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable { onItemClick(item) }
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
    onImageClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val displayFolders = state.displayFolders
    val displayImages = state.displayImages

    if (displayFolders.isEmpty() && displayImages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isSearchActive) {
                Text("検索結果が見つかりませんでした")
            } else if (state.imageLoadError != null) {
                Text(
                    text = state.imageLoadError,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text("このフォルダにフォルダや画像はありません")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(fraction = 5f / 6f),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onBack) {
                    Text("戻る")
                }
            }
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
    val folderCount = displayFolders.size
    LaunchedEffect(lastVisibleIndex, displayImages.size) {
        val lastVisibleImageIndex = lastVisibleIndex - folderCount
        val prefetchStart = (lastVisibleImageIndex + 1).coerceAtLeast(0)
        val prefetchEnd = (prefetchStart + prefetchRange).coerceAtMost(displayImages.size)
        for (i in prefetchStart until prefetchEnd) {
            val request = ImageRequest.Builder(context)
                .data(SmbImageRequest(path = displayImages[i].path, thumbnail = true, thumbnailSizePx = cellSizePx))
                .size(cellSizePx)
                .build()
            imageLoader.enqueue(request)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Image load error banner
            if (state.imageLoadError != null) {
                item(
                    key = "imageLoadError",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Text(
                        text = "画像の読み込みに失敗しました: ${state.imageLoadError}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Folders as full-width list items
            items(
                items = displayFolders,
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
                items = displayImages,
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

        // Scrollbar
        GridScrollbar(gridState = gridState)
    }
}

@Composable
private fun GridScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val totalItems by remember {
        derivedStateOf { gridState.layoutInfo.totalItemsCount }
    }
    val canScroll by remember {
        derivedStateOf {
            gridState.layoutInfo.totalItemsCount > 0 &&
                gridState.layoutInfo.visibleItemsInfo.size < gridState.layoutInfo.totalItemsCount
        }
    }
    val scrollFraction by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) return@derivedStateOf 0f
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 0f
            val firstVisibleIndex = visibleItems.first().index.toFloat()
            val totalCount = layoutInfo.totalItemsCount.toFloat()
            val visibleCount = visibleItems.size.toFloat()
            if (totalCount <= visibleCount) 0f
            else (firstVisibleIndex / (totalCount - visibleCount)).coerceIn(0f, 1f)
        }
    }

    AnimatedVisibility(
        visible = canScroll,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val thumbHeightDp = 48.dp
            val trackPaddingDp = 4.dp
            var trackHeightPx by remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current
            val thumbHeightPx = remember(density) { with(density) { thumbHeightDp.toPx() } }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(24.dp)
                    .padding(vertical = trackPaddingDp)
                    .onSizeChanged { trackHeightPx = it.height.toFloat() }
                    .pointerInput(totalItems) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            val trackUsable = trackHeightPx - thumbHeightPx
                            if (trackUsable <= 0f) return@detectVerticalDragGestures
                            val y = change.position.y - thumbHeightPx / 2f
                            val fraction = (y / trackUsable).coerceIn(0f, 1f)
                            val layoutInfo = gridState.layoutInfo
                            val totalCount = layoutInfo.totalItemsCount
                            val visibleCount = layoutInfo.visibleItemsInfo.size
                            val targetIndex = (fraction * (totalCount - visibleCount))
                                .roundToInt()
                                .coerceIn(0, (totalCount - 1).coerceAtLeast(0))
                            coroutineScope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        }
                    }
            ) {
                // Track
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(3.dp)
                        )
                )

                // Thumb
                val thumbOffsetPx = ((trackHeightPx - thumbHeightPx) * scrollFraction).roundToInt()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, thumbOffsetPx) }
                        .width(6.dp)
                        .height(thumbHeightDp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}
