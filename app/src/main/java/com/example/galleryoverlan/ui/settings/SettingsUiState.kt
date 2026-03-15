package com.example.galleryoverlan.ui.settings

data class SettingsUiState(
    val hostName: String = "",
    val shareName: String = "",
    val userName: String = "",
    val password: String = "",
    val baseFolderPath: String = "",
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val testResult: TestResult? = null,
    val isSaved: Boolean = false
)

sealed class TestResult {
    data object Success : TestResult()
    data class Failure(val message: String) : TestResult()
}
