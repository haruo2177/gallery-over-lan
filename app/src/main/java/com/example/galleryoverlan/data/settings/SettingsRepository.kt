package com.example.galleryoverlan.data.settings

import com.example.galleryoverlan.domain.model.ConnectionConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val connectionConfig: Flow<ConnectionConfig?>
    suspend fun saveConnectionConfig(config: ConnectionConfig)
    suspend fun updateLastSuccessfulIp(ip: String)
    suspend fun clearAll()
}
