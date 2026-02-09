package com.olnatura.qr.data.device

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.olnatura.qr.core.datastore.appDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

class DeviceIdProvider(private val context: Context) {
    private val KEY = stringPreferencesKey("device_id")

    suspend fun getOrCreate(): String {
        val prefs = context.appDataStore.data.first()
        val existing = prefs[KEY]
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString()
        context.appDataStore.edit { it[KEY] = newId }
        return newId
    }
}