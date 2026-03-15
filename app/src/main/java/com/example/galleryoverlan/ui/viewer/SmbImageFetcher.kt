package com.example.galleryoverlan.ui.viewer

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.galleryoverlan.data.smb.SmbRepository
import okio.buffer
import okio.source

data class SmbImageRequest(val path: String)

class SmbImageFetcher(
    private val data: SmbImageRequest,
    private val smbRepository: SmbRepository,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val result = smbRepository.readImageStream(data.path)
        val inputStream = result.getOrThrow()

        return SourceResult(
            source = ImageSource(
                source = inputStream.source().buffer(),
                context = context
            ),
            mimeType = null,
            dataSource = DataSource.NETWORK
        )
    }

    class Factory(
        private val smbRepository: SmbRepository,
        private val context: Context
    ) : Fetcher.Factory<SmbImageRequest> {
        override fun create(
            data: SmbImageRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return SmbImageFetcher(data, smbRepository, context)
        }
    }
}
