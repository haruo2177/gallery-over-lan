package com.example.galleryoverlan.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.example.galleryoverlan.data.cache.ThumbnailCache
import com.example.galleryoverlan.data.smb.SmbRepository
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

data class SmbImageRequest(
    val path: String,
    val thumbnail: Boolean = false,
    val thumbnailSizePx: Int = DEFAULT_thumbnailSize
) {
    companion object {
        const val DEFAULT_thumbnailSize = 128
    }
}

class SmbImageFetcher(
    private val data: SmbImageRequest,
    private val smbRepository: SmbRepository,
    private val thumbnailCache: ThumbnailCache,
    private val context: Context
) : Fetcher {

    private val thumbnailSize: Int = data.thumbnailSizePx

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
            // EXIFから回転情報を取得
            val exif = ExifInterface(ByteArrayInputStream(originalBytes))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            // Decode with inSampleSize for memory efficiency
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, options)

            val sampleSize = calculateInSampleSize(
                options.outWidth, options.outHeight,
                thumbnailSize, thumbnailSize
            )

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            var bitmap = BitmapFactory.decodeByteArray(
                originalBytes, 0, originalBytes.size, decodeOptions
            ) ?: return null

            // EXIF回転を適用
            bitmap = applyExifRotation(bitmap, orientation)

            // 短辺に合わせて上部から正方形に切り出し（顔は上部にある想定）
            val w = bitmap.width
            val h = bitmap.height
            val cropSize = minOf(w, h)
            val x = (w - cropSize) / 2  // 水平方向は中央
            val y = 0                    // 垂直方向は上端から

            val cropped = Bitmap.createBitmap(bitmap, x, y, cropSize, cropSize)
            if (cropped != bitmap) bitmap.recycle()

            val scaled = Bitmap.createScaledBitmap(cropped, thumbnailSize, thumbnailSize, true)
            if (scaled != cropped) cropped.recycle()

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
            scaled.recycle()
            output.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
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

    class SmbImageKeyer : Keyer<SmbImageRequest> {
        override fun key(data: SmbImageRequest, options: Options): String {
            return if (data.thumbnail) "smb_thumb:${data.thumbnailSizePx}:${data.path}" else "smb_full:${data.path}"
        }
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
