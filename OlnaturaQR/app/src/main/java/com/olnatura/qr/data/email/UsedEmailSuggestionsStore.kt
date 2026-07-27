package com.olnatura.qr.data.email

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.olnatura.qr.core.datastore.appDataStore
import kotlinx.coroutines.flow.first

class UsedEmailSuggestionsStore(private val context: Context) {

    private val KEY = stringSetPreferencesKey("used_email_suggestions_v1")

    suspend fun loadUsed(): Set<String> {
        val prefs = context.appDataStore.data.first()
        return prefs[KEY].orEmpty().map { it.lowercase() }.toSet()
    }

    suspend fun markUsed(email: String) {
        val normalized = email.trim().lowercase()
        if (normalized.isEmpty()) return
        context.appDataStore.edit { prefs ->
            val current = prefs[KEY].orEmpty().toMutableSet()
            current.add(normalized)
            prefs[KEY] = current
        }
    }
}
