package com.example.galleryoverlan.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailCacheImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThumbnailCache {

    companion object {
        private const val CACHE_DIR_NAME = "thumbnails"
        private const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024 // 500MB
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).also { it.mkdirs() }
    }

    override fun get(imagePath: String): File? {
        val file = cacheFile(imagePath)
        return if (file.exists()) file else null
    }

    override fun put(imagePath: String, data: ByteArray): File {
        val file = cacheFile(imagePath)
        file.writeBytes(data)
        trimIfNeeded()
        return file
    }

    override fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun cacheFile(imagePath: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(imagePath.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$hash.jpg")
    }

    private fun trimIfNeeded() {
        val files = cacheDir.listFiles() ?: return
        val totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_CACHE_SIZE_BYTES) return

        // Delete oldest files first
        val sorted = files.sortedBy { it.lastModified() }
        var freed = 0L
        val target = totalSize - MAX_CACHE_SIZE_BYTES
        for (file in sorted) {
            if (freed >= target) break
            freed += file.length()
            file.delete()
        }
    }
}
