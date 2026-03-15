package com.example.galleryoverlan.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.galleryoverlan.data.cache.ThumbnailCache
import com.example.galleryoverlan.data.smb.SmbRepository
import okio.buffer
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File

data class SmbImageRequest(
    val path: String,
    val thumbnail: Boolean = false
)

class SmbImageFetcher(
    private val data: SmbImageRequest,
    private val smbRepository: SmbRepository,
    private val thumbnailCache: ThumbnailCache,
    private val context: Context
) : Fetcher {

    companion object {
        private const val THUMBNAIL_SIZE = 128
    }

    override suspend fun fetch(): FetchResult {
        // Check thumbnail cache first
        if (data.thumbnail) {
            val cached = thumbnailCache.get(data.path)
            if (cached != null) {
                return fileResult(cached)
            }
        }

        val result = smbRepository.readImageStream(data.path)
        val inputStream = result.getOrThrow()
        val bytes = inputStream.use { it.readBytes() }

        // Generate and cache thumbnail
        if (data.thumbnail) {
            val thumbnailBytes = createThumbnail(bytes)
            if (thumbnailBytes != null) {
                val file = thumbnailCache.put(data.path, thumbnailBytes)
                return fileResult(file)
            }
        }

        return SourceResult(
            source = ImageSource(
                source = bytes.inputStream().source().buffer(),
                context = context
            ),
            mimeType = null,
            dataSource = DataSource.NETWORK
        )
    }

    private fun fileResult(file: File): SourceResult {
        return SourceResult(
            source = ImageSource(
                source = file.inputStream().source().buffer(),
                context = context
            ),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK
        )
    }

    private fun createThumbnail(originalBytes: ByteArray): ByteArray? {
        return try {
            // Decode with inSampleSize for memory efficiency
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, options)

            val sampleSize = calculateInSampleSize(
                options.outWidth, options.outHeight,
                THUMBNAIL_SIZE, THUMBNAIL_SIZE
            )

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(
                originalBytes, 0, originalBytes.size, decodeOptions
            ) ?: return null

            val scaled = Bitmap.createScaledBitmap(
                bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true
            )
            if (scaled != bitmap) bitmap.recycle()

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
            scaled.recycle()
            output.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    class Factory(
        private val smbRepository: SmbRepository,
        private val thumbnailCache: ThumbnailCache,
        private val context: Context
    ) : Fetcher.Factory<SmbImageRequest> {
        override fun create(
            data: SmbImageRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return SmbImageFetcher(data, smbRepository, thumbnailCache, context)
        }
    }
}
