package com.olnatura.qr.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.olnatura.qr.core.datastore.appDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieJar(private val context: Context) : CookieJar {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, StoredCookie::class.java)
    private val adapter = moshi.adapter<List<StoredCookie>>(listType)

    private val memory = ConcurrentHashMap<String, List<Cookie>>() // host -> cookies
    private val KEY_COOKIES = stringPreferencesKey("cookies_v1")

    init {
        runBlocking { loadFromDisk() }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val valid = cookies.filter { it.expiresAt > System.currentTimeMillis() }
        memory[host] = mergeCookies(memory[host].orEmpty(), valid)
        runBlocking { persistToDisk() }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        val cookies = memory[host].orEmpty().filter { it.expiresAt > now }
        memory[host] = cookies
        return cookies
    }

    fun clearAll() {
        memory.clear()
        runBlocking {
            context.appDataStore.edit { it.remove(KEY_COOKIES) }
        }
    }

    private suspend fun loadFromDisk() {
        val prefs = context.appDataStore.data.first()
        val raw = prefs[KEY_COOKIES] ?: return
        val stored = runCatching { adapter.fromJson(raw) }.getOrNull().orEmpty()
        val grouped = stored.groupBy { it.host }.mapValues { (_, items) ->
            items.mapNotNull { it.toCookieOrNull() }
                .filter { it.expiresAt > System.currentTimeMillis() }
        }
        memory.putAll(grouped)
    }

    private suspend fun persistToDisk() {
        val allStored = memory.flatMap { (host, cookies) ->
            cookies.map { StoredCookie.from(host, it) }
        }
        val json = adapter.toJson(allStored)
        context.appDataStore.edit { it[KEY_COOKIES] = json }
    }

    private fun mergeCookies(old: List<Cookie>, new: List<Cookie>): List<Cookie> {
        val map = LinkedHashMap<String, Cookie>()
        fun key(c: Cookie) = "${c.name}|${c.domain}|${c.path}"
        old.forEach { map[key(it)] = it }
        new.forEach { map[key(it)] = it }
        return map.values.toList()
    }
}

data class StoredCookie(
    val host: String,
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean
) {
    fun toCookieOrNull(): Cookie? = try {
        val b = Cookie.Builder()
            .name(name).value(value)
            .domain(domain).path(path)
            .expiresAt(expiresAt)
        if (secure) b.secure()
        if (httpOnly) b.httpOnly()
        b.build()
    } catch (_: Exception) { null }

    companion object {
        fun from(host: String, c: Cookie) = StoredCookie(
            host = host,
            name = c.name,
            value = c.value,
            domain = c.domain,
            path = c.path,
            expiresAt = c.expiresAt,
            secure = c.secure,
            httpOnly = c.httpOnly
        )
    }
}