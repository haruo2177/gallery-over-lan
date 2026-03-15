package com.example.galleryoverlan.di

import com.example.galleryoverlan.core.dispatchers.AppDispatchers
import com.example.galleryoverlan.core.dispatchers.DefaultAppDispatchers
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DispatcherModule {

    @Binds
    @Singleton
    abstract fun bindAppDispatchers(impl: DefaultAppDispatchers): AppDispatchers
}
