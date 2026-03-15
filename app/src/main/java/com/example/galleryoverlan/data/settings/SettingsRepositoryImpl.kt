package com.example.galleryoverlan.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.galleryoverlan.domain.model.ConnectionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val KEY_HOST_NAME = stringPreferencesKey("host_name")
        private val KEY_SHARE_NAME = stringPreferencesKey("share_name")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_BASE_FOLDER_PATH = stringPreferencesKey("base_folder_path")
        private val KEY_LAST_SUCCESSFUL_IP = stringPreferencesKey("last_successful_ip")
    }

    override val connectionConfig: Flow<ConnectionConfig?> = dataStore.data.map { prefs ->
        val hostName = prefs[KEY_HOST_NAME] ?: return@map null
        val shareName = prefs[KEY_SHARE_NAME] ?: return@map null
        val userName = prefs[KEY_USER_NAME] ?: return@map null
        ConnectionConfig(
            hostName = hostName,
            shareName = shareName,
            userName = userName,
            baseFolderPath = prefs[KEY_BASE_FOLDER_PATH] ?: "",
            lastSuccessfulIp = prefs[KEY_LAST_SUCCESSFUL_IP]
        )
    }

    override suspend fun saveConnectionConfig(config: ConnectionConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_HOST_NAME] = config.hostName
            prefs[KEY_SHARE_NAME] = config.shareName
            prefs[KEY_USER_NAME] = config.userName
            prefs[KEY_BASE_FOLDER_PATH] = config.baseFolderPath
            config.lastSuccessfulIp?.let { prefs[KEY_LAST_SUCCESSFUL_IP] = it }
        }
    }

    override suspend fun updateLastSuccessfulIp(ip: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SUCCESSFUL_IP] = ip
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
