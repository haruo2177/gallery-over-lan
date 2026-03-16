package com.example.galleryoverlan.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.galleryoverlan.domain.model.SlideshowState

@Composable
fun ViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.stopSlideshow()
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (state.error != null) {
            Text(
                text = state.error ?: "",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else if (state.images.isNotEmpty()) {
            val pagerState = rememberPagerState(
                initialPage = state.currentIndex,
                pageCount = { state.images.size }
            )

            // Sync pager with viewmodel state (for slideshow auto-advance)
            LaunchedEffect(state.currentIndex) {
                if (pagerState.currentPage != state.currentIndex) {
                    pagerState.animateScrollToPage(state.currentIndex)
                }
            }

            // Sync viewmodel with user swipe
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .collect { viewModel.onPageChanged(it) }
            }

            val tapAreaRatio = 0.25f

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset: Offset ->
                                val width = size.width.toFloat()
                                when {
                                    offset.x < width * tapAreaRatio -> {
                                        // 左1/4タップ → 前の画像
                                        val prev = pagerState.currentPage - 1
                                        if (prev >= 0) {
                                            viewModel.onPageChanged(prev)
                                        }
                                    }
                                    offset.x > width * (1f - tapAreaRatio) -> {
                                        // 右1/4タップ → 次の画像
                                        val next = pagerState.currentPage + 1
                                        if (next < state.images.size) {
                                            viewModel.onPageChanged(next)
                                        }
                                    }
                                    else -> {
                                        // 中央タップ → コントロール表示切替
                                        viewModel.toggleControls()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = SmbImageRequest(state.images[page].path),
                        contentDescription = state.images[page].name,
                        contentScale = ContentScale.Fit,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color.White
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top info overlay
            AnimatedVisibility(
                visible = state.showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.positionText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = state.currentImage?.name ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Bottom controls overlay
            AnimatedVisibility(
                visible = state.showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Interval picker button
                    IconButton(onClick = viewModel::toggleIntervalPicker) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = "間隔設定",
                            tint = Color.White
                        )
                    }
                    // Play/Pause button
                    IconButton(onClick = viewModel::toggleSlideshow) {
                        Icon(
                            imageVector = if (state.isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = if (state.isPlaying) "一時停止" else "スライドショー開始",
                            tint = Color.White
                        )
                    }
                    // Stop button (visible when not idle)
                    if (state.slideshowState !is SlideshowState.Idle) {
                        IconButton(onClick = viewModel::stopSlideshow) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "停止",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Interval picker dialog
    if (state.showIntervalPicker) {
        IntervalPickerDialog(
            currentIntervalMs = state.slideshowIntervalMs,
            onIntervalSelected = { viewModel.onIntervalChange(it) },
            onDismiss = viewModel::toggleIntervalPicker
        )
    }
}

@Composable
private fun IntervalPickerDialog(
    currentIntervalMs: Long,
    onIntervalSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentIntervalMs / 1000f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("スライドショー間隔") },
        text = {
            Column {
                Text("${sliderValue.toInt()}秒")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..30f,
                    steps = 28
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1秒", style = MaterialTheme.typography.labelSmall)
                    Text("30秒", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onIntervalSelected(sliderValue.toLong() * 1000)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
