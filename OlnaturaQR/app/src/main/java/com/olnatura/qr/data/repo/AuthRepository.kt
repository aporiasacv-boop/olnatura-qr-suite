package com.olnatura.qr.data.repo

import com.olnatura.qr.data.model.LoginRequest
import com.olnatura.qr.data.model.MeResponse
import com.olnatura.qr.data.model.RequestAccessRequest
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

    suspend fun requestAccess(username: String, email: String, password: String, roleRequested: String): Result<Unit> {
        val resp = api.requestAccess(
            RequestAccessRequest(username = username, email = email, password = password, roleRequested = roleRequested)
        )
        return when {
            resp.isSuccessful -> Result.success(Unit)
            resp.code() == 409 -> Result.failure(RuntimeException("Usuario o email ya existe"))
            else -> Result.failure(RuntimeException(resp.message() ?: "Error ${resp.code()}"))
        }
    }

    fun clearSession() = cookieJar.clearAll()
}