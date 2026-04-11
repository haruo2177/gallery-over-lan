package com.example.galleryoverlan.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.domain.model.SlideshowState
import com.example.galleryoverlan.domain.usecase.ListImagesUseCase
import com.example.galleryoverlan.domain.usecase.SlideshowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listImagesUseCase: ListImagesUseCase,
    private val slideshowUseCase: SlideshowUseCase,
    private val smbRepository: SmbRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val folderPath: String = com.example.galleryoverlan.ui.navigation.Routes.decodePath(
        savedStateHandle.get<String>("folderPath")
    )
    private val startIndex: Int = savedStateHandle["startIndex"] ?: 0
    private val autoSlideshow: Boolean = savedStateHandle["autoSlideshow"] ?: false

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var slideshowJob: Job? = null

    init {
        loadImages()
    }

    private fun loadImages() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            doLoadImages()

            if (autoSlideshow && _uiState.value.images.isNotEmpty()) {
                startSlideshow()
            }
        }
    }

    private suspend fun doLoadImages() {
        val result = listImagesUseCase(folderPath)
        _uiState.value = when (result) {
            is AppResult.Success -> _uiState.value.copy(
                images = result.data,
                currentIndex = startIndex.coerceIn(0, (result.data.size - 1).coerceAtLeast(0)),
                isLoading = false,
                error = null
            )
            is AppResult.Error -> _uiState.value.copy(
                isLoading = false,
                error = result.message
            )
        }
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(dispatchers.io) {
            delay(500)
            doLoadImages()
        }
    }

    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }

    fun toggleSlideshow() {
        val state = _uiState.value
        when (state.slideshowState) {
            is SlideshowState.Idle, is SlideshowState.Paused -> startSlideshow()
            is SlideshowState.Playing -> pauseSlideshow()
        }
    }

    fun stopSlideshow() {
        slideshowJob?.cancel()
        slideshowJob = null
        _uiState.value = _uiState.value.copy(
            slideshowState = SlideshowState.Idle,
            showControls = true
        )
    }

    fun onIntervalChange(intervalMs: Long) {
        _uiState.value = _uiState.value.copy(slideshowIntervalMs = intervalMs)
        if (_uiState.value.isPlaying) {
            startSlideshow()
        }
    }

    fun dismissSlideshowEndDialog() {
        _uiState.value = _uiState.value.copy(showSlideshowEndDialog = false)
    }

    fun toggleIntervalPicker() {
        _uiState.value = _uiState.value.copy(
            showIntervalPicker = !_uiState.value.showIntervalPicker
        )
    }

    private fun startSlideshow() {
        slideshowJob?.cancel()
        val state = _uiState.value
        val intervalMs = state.slideshowIntervalMs

        _uiState.value = state.copy(
            slideshowState = SlideshowState.Playing(intervalMs),
            showControls = false
        )

        slideshowJob = viewModelScope.launch {
            slideshowUseCase.start(
                totalImages = state.images.size,
                startIndex = state.currentIndex,
                intervalMs = intervalMs
            ).collect { nextIndex ->
                _uiState.value = _uiState.value.copy(currentIndex = nextIndex)
            }
            // Flow completed — reached the last image
            slideshowJob = null
            _uiState.value = _uiState.value.copy(
                slideshowState = SlideshowState.Idle,
                showControls = true,
                showSlideshowEndDialog = true
            )
        }
    }

    private fun pauseSlideshow() {
        slideshowJob?.cancel()
        slideshowJob = null
        _uiState.value = _uiState.value.copy(
            slideshowState = SlideshowState.Paused,
            showControls = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        slideshowJob?.cancel()
    }
}
