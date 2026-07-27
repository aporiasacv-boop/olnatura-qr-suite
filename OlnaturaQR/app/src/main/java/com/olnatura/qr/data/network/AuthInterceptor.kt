package com.olnatura.qr.data.network

import com.olnatura.qr.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val cookieJar: PersistentCookieJar
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            val path = chain.request().url.encodedPath
            // Un login fallido no debe borrar una cookie de sesión aún válida ni forzar navegación.
            if (!path.endsWith("/auth/login")) {
                cookieJar.clearAll()
                sessionManager.onUnauthorized()
            }
        }

        return response
    }
}