package com.example.galleryoverlan.data.cache

import java.io.File

interface ThumbnailCache {
    fun get(imagePath: String): File?
    fun put(imagePath: String, data: ByteArray): File
    fun clear()
}
