package com.olnatura.qr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.olnatura.qr.core.Constants
import com.olnatura.qr.core.session.SessionManager
import com.olnatura.qr.data.device.DeviceIdProvider
import com.olnatura.qr.data.network.ApiClient
import com.olnatura.qr.data.network.PersistentCookieJar
import com.olnatura.qr.data.repo.AdminLotRepository
import com.olnatura.qr.data.repo.AuthRepository
import com.olnatura.qr.data.repo.CommentRepository
import com.olnatura.qr.data.repo.QrRepository
import com.olnatura.qr.data.repo.ScanRepository
import com.olnatura.qr.ui.navigation.AppNavGraph
import com.olnatura.qr.ui.screen.login.LoginViewModel
import com.olnatura.qr.ui.screen.requestaccess.RequestAccessViewModel
import com.olnatura.qr.ui.screen.report.ReportProblemViewModel
import com.olnatura.qr.ui.screen.result.ResultViewModel
import com.olnatura.qr.ui.screen.scanner.ScannerViewModel
import com.olnatura.qr.ui.share.SharePayload
import com.olnatura.qr.ui.sheet.ShareBottomSheet
import com.olnatura.qr.ui.theme.OlnaturaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(applicationContext)
        val cookieJar = PersistentCookieJar(applicationContext)

        val api = ApiClient.create(
            baseUrl = Constants.BASE_URL,
            cookieJar = cookieJar,
            sessionManager = sessionManager
        )

        val authRepo = AuthRepository(api, cookieJar)
        val qrRepo = QrRepository(api)
        val deviceIdProvider = DeviceIdProvider(applicationContext)
        val scanRepo = ScanRepository(api, deviceIdProvider)
        val commentRepo = CommentRepository(api)
        val adminLotRepo = AdminLotRepository(api)
        val loginVm = LoginViewModel(authRepo)
        val usedEmailStore = com.olnatura.qr.data.email.UsedEmailSuggestionsStore(applicationContext)
        val requestAccessVm = RequestAccessViewModel(authRepo, usedEmailStore)
        val scannerVm = ScannerViewModel()
        val reportVm = ReportProblemViewModel()

        setContent {
            OlnaturaTheme {
                // Evita que la taskbar / barra de navegación de la tablet tape botones.
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    var shareOpen by remember { mutableStateOf(false) }
                    var sharePayload by remember { mutableStateOf<SharePayload?>(null) }

                    AppNavGraph(
                        sessionManager = sessionManager,
                        authRepo = authRepo,
                        cookieJar = cookieJar,
                        loginVm = loginVm,
                        requestAccessVm = requestAccessVm,
                        scannerVm = scannerVm,
                        resultVmFactory = { ResultViewModel(authRepo, qrRepo, scanRepo, commentRepo, adminLotRepo) },
                        reportVm = reportVm,
                        onShare = { payload ->
                            sharePayload = payload
                            shareOpen = true
                        }
                    )

                    if (shareOpen && sharePayload != null) {
                        ShareBottomSheet(
                            payload = sharePayload!!,
                            onDismiss = {
                                shareOpen = false
                                sharePayload = null
                            }
                        )
                    }
                }
            }
        }
    }
}
