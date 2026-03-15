package com.example.galleryoverlan.data.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager
) : CredentialRepository {

    companion object {
        private val KEY_ENCRYPTED_USERNAME = stringPreferencesKey("encrypted_username")
        private val KEY_ENCRYPTED_PASSWORD = stringPreferencesKey("encrypted_password")
    }

    override suspend fun saveCredentials(userName: String, password: String) {
        val encryptedUsername = cryptoManager.encrypt(userName.toByteArray(Charsets.UTF_8))
        val encryptedPassword = cryptoManager.encrypt(password.toByteArray(Charsets.UTF_8))
        dataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_USERNAME] = Base64.encodeToString(encryptedUsername, Base64.NO_WRAP)
            prefs[KEY_ENCRYPTED_PASSWORD] = Base64.encodeToString(encryptedPassword, Base64.NO_WRAP)
        }
    }

    override suspend fun getCredentials(): Pair<String, String>? {
        val prefs = dataStore.data.map { prefs ->
            val encUsername = prefs[KEY_ENCRYPTED_USERNAME] ?: return@map null
            val encPassword = prefs[KEY_ENCRYPTED_PASSWORD] ?: return@map null
            val username = String(
                cryptoManager.decrypt(Base64.decode(encUsername, Base64.NO_WRAP)),
                Charsets.UTF_8
            )
            val password = String(
                cryptoManager.decrypt(Base64.decode(encPassword, Base64.NO_WRAP)),
                Charsets.UTF_8
            )
            Pair(username, password)
        }.first()
        return prefs
    }

    override suspend fun clearCredentials() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ENCRYPTED_USERNAME)
            prefs.remove(KEY_ENCRYPTED_PASSWORD)
        }
    }
}
