package com.olnatura.qr.ui.screen.boot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olnatura.qr.data.network.PersistentCookieJar
import com.olnatura.qr.data.repo.AuthRepository
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen
import retrofit2.HttpException

sealed class SessionBootstrapResult {
    data object GoScanner : SessionBootstrapResult()
    data object GoLogin : SessionBootstrapResult()
}

@Composable
fun SessionBootstrapScreen(
    authRepo: AuthRepository,
    cookieJar: PersistentCookieJar,
    onFinished: (SessionBootstrapResult) -> Unit
) {
    LaunchedEffect(Unit) {
        if (!cookieJar.hasSessionCookie()) {
            onFinished(SessionBootstrapResult.GoLogin)
            return@LaunchedEffect
        }
        try {
            authRepo.me()
            onFinished(SessionBootstrapResult.GoScanner)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                authRepo.clearSession()
            }
            onFinished(SessionBootstrapResult.GoLogin)
        } catch (_: Exception) {
            // Red/timeout: conservar cookie para reintentar en el próximo arranque.
            onFinished(SessionBootstrapResult.GoLogin)
        }
    }

    Surface(color = OlnCream, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = OlnGreen)
                Text("Restaurando sesión…", modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
