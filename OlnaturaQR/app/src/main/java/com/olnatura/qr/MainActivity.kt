package com.olnatura.qr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.olnatura.qr.core.Constants
import com.olnatura.qr.core.session.SessionManager
import com.olnatura.qr.data.device.DeviceIdProvider
import com.olnatura.qr.data.network.ApiClient
import com.olnatura.qr.data.network.PersistentCookieJar
import com.olnatura.qr.data.repo.AuthRepository
import com.olnatura.qr.data.repo.QrRepository
import com.olnatura.qr.data.repo.ScanRepository
import com.olnatura.qr.ui.navigation.AppNavGraph
import com.olnatura.qr.ui.screen.login.LoginViewModel
import com.olnatura.qr.ui.screen.requestaccess.RequestAccessViewModel
import com.olnatura.qr.ui.screen.report.ReportProblemViewModel
import com.olnatura.qr.ui.screen.result.ResultViewModel
import com.olnatura.qr.ui.screen.scanner.ScannerViewModel
import com.olnatura.qr.ui.sheet.ShareBottomSheet
import com.olnatura.qr.ui.theme.OlnaturaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Usa applicationContext para todo lo que vive más que la Activity
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

        // VMs (simples, sin DI)
        val loginVm = LoginViewModel(authRepo)
        val requestAccessVm = RequestAccessViewModel(authRepo)
        val scannerVm = ScannerViewModel()
        val reportVm = ReportProblemViewModel()

        setContent {
            OlnaturaTheme {
                var shareOpen by remember { mutableStateOf(false) }
                var shareLote by remember { mutableStateOf("") }
                var shareStatus by remember { mutableStateOf("") }

                AppNavGraph(
                    sessionManager = sessionManager,
                    loginVm = loginVm,
                    requestAccessVm = requestAccessVm,
                    scannerVm = scannerVm,
                    resultVmFactory = { ResultViewModel(authRepo, qrRepo, scanRepo) },
                    reportVm = reportVm,
                    onShare = { lote, status ->
                        shareLote = lote
                        shareStatus = status
                        shareOpen = true
                    }
                )

                if (shareOpen) {
                    ShareBottomSheet(
                        lote = shareLote,
                        status = shareStatus,
                        onDismiss = { shareOpen = false }
                    )
                }
            }
        }
    }
}