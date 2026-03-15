package com.example.galleryoverlan.ui.navigation

object Routes {
    const val SETTINGS = "settings"
    const val BROWSER = "browser"
    const val IMAGE_LIST = "imageList/{folderPath}"
    const val VIEWER = "viewer/{folderPath}/{startIndex}"

    fun imageList(folderPath: String): String =
        "imageList/${folderPath.ifEmpty { " " }}"

    fun viewer(folderPath: String, startIndex: Int): String =
        "viewer/${folderPath.ifEmpty { " " }}/$startIndex"
}
