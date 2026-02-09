package com.olnatura.qr.data.repo

import com.olnatura.qr.data.model.LoginRequest
import com.olnatura.qr.data.model.MeResponse
import com.olnatura.qr.data.network.OlnaturaApi
import com.olnatura.qr.data.network.PersistentCookieJar
import retrofit2.HttpException

class AuthRepository(
    private val api: OlnaturaApi,
    private val cookieJar: PersistentCookieJar
) {
    suspend fun login(username: String, password: String): Result<Unit> {
        val resp = api.login(LoginRequest(username, password))

        return when {
            resp.isSuccessful -> Result.success(Unit)
            resp.code() == 401 -> Result.failure(HttpException(resp))
            else -> Result.failure(RuntimeException("Login failed: ${resp.code()}"))
        }
    }

    suspend fun me(): MeResponse = api.me()

    suspend fun logout() {
        runCatching { api.logout() }
        cookieJar.clearAll()
    }

    fun clearSession() = cookieJar.clearAll()
}