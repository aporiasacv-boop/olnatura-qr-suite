package com.olnatura.qr.core.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SavedAccountStore(context: Context) {

    data class SavedAccount(val username: String, val password: String)

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(username: String, password: String) {
        val id = username.trim()
        if (id.isEmpty() || password.isEmpty()) return
        prefs.edit()
            .putString(KEY_USERNAME, id)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun load(): SavedAccount? {
        val username = prefs.getString(KEY_USERNAME, null)?.trim().orEmpty()
        val password = prefs.getString(KEY_PASSWORD, null).orEmpty()
        if (username.isEmpty() || password.isEmpty()) return null
        return SavedAccount(username = username, password = password)
    }

    fun clear() {
        prefs.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD).apply()
    }

    companion object {
        private const val PREFS_NAME = "olnatura_saved_account"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
