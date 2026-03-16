package com.example.galleryoverlan.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val CONNECT = "connect"
    const val BROWSE = "browse"
    const val VIEWER = "viewer/{folderPath}/{startIndex}"

    fun viewer(folderPath: String, startIndex: Int): String =
        "viewer/${encode(folderPath)}/$startIndex"

    fun decodePath(encoded: String?): String =
        if (encoded.isNullOrBlank()) "" else URLDecoder.decode(encoded, "UTF-8")

    private fun encode(value: String): String =
        URLEncoder.encode(value.ifEmpty { " " }, "UTF-8")
}
