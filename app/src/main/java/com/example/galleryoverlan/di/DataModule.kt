package com.example.galleryoverlan.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import com.example.galleryoverlan.data.security.CredentialRepository
import com.example.galleryoverlan.data.security.CredentialRepositoryImpl
import com.example.galleryoverlan.data.settings.SettingsRepository
import com.example.galleryoverlan.data.settings.SettingsRepositoryImpl
import com.example.galleryoverlan.data.smb.DefaultHostResolver
import com.example.galleryoverlan.data.smb.HostResolver
import com.example.galleryoverlan.data.smb.SmbClient
import com.example.galleryoverlan.data.smb.SmbClientImpl
import com.example.galleryoverlan.data.smb.SmbRepository
import com.example.galleryoverlan.data.smb.SmbRepositoryImpl
import com.example.galleryoverlan.ui.viewer.SmbImageFetcher
import com.example.galleryoverlan.ui.viewer.SmbImageRequest
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSmbClient(impl: SmbClientImpl): SmbClient

    @Binds
    @Singleton
    abstract fun bindSmbRepository(impl: SmbRepositoryImpl): SmbRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCredentialRepository(impl: CredentialRepositoryImpl): CredentialRepository

    @Binds
    @Singleton
    abstract fun bindHostResolver(impl: DefaultHostResolver): HostResolver
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        smbRepository: SmbRepository
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(SmbImageFetcher.Factory(smbRepository, context))
            }
            .crossfade(true)
            .build()
    }
}
