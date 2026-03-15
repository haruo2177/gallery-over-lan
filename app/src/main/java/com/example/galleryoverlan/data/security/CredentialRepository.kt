package com.example.galleryoverlan.data.security

interface CredentialRepository {
    suspend fun saveCredentials(userName: String, password: String)
    suspend fun getCredentials(): Pair<String, String>?
    suspend fun clearCredentials()
}
