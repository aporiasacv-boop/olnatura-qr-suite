package com.olnatura.qr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.olnatura.qr.core.session.SessionManager
import com.olnatura.qr.data.network.PersistentCookieJar
import com.olnatura.qr.data.repo.AuthRepository
import com.olnatura.qr.ui.screen.boot.SessionBootstrapResult
import com.olnatura.qr.ui.screen.boot.SessionBootstrapScreen
import com.olnatura.qr.ui.screen.login.LoginScreen
import com.olnatura.qr.ui.screen.login.LoginViewModel
import com.olnatura.qr.ui.screen.requestaccess.RequestAccessScreen
import com.olnatura.qr.ui.screen.requestaccess.RequestAccessViewModel
import com.olnatura.qr.ui.screen.report.ReportMode
import com.olnatura.qr.ui.screen.report.ReportProblemScreen
import com.olnatura.qr.ui.screen.report.ReportProblemViewModel
import com.olnatura.qr.ui.screen.result.ResultScreen
import com.olnatura.qr.ui.screen.result.ResultViewModel
import com.olnatura.qr.ui.screen.scanner.ScannerScreen
import com.olnatura.qr.ui.screen.scanner.ScannerViewModel
import com.olnatura.qr.ui.share.SharePayload
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    sessionManager: SessionManager,
    authRepo: AuthRepository,
    cookieJar: PersistentCookieJar,
    loginVm: LoginViewModel,
    requestAccessVm: RequestAccessViewModel,
    scannerVm: ScannerViewModel,
    resultVmFactory: () -> ResultViewModel,
    reportVm: ReportProblemViewModel,
    onShare: (SharePayload) -> Unit
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    fun goLoginClearingBackStack() {
        nav.navigate(Route.Login.path) { popUpTo(0) { inclusive = true } }
    }

    fun goScannerClearingBackStack() {
        nav.navigate(Route.Scanner.path) { popUpTo(0) { inclusive = true } }
    }

    fun logoutAndGoLogin() {
        scope.launch {
            authRepo.logout()
            goLoginClearingBackStack()
        }
    }

    LaunchedEffect(Unit) {
        sessionManager.unauthorized.collect {
            goLoginClearingBackStack()
        }
    }

    NavHost(navController = nav, startDestination = Route.Boot.path) {
        composable(Route.Boot.path) {
            SessionBootstrapScreen(
                authRepo = authRepo,
                cookieJar = cookieJar,
                onFinished = { result ->
                    when (result) {
                        SessionBootstrapResult.GoScanner -> goScannerClearingBackStack()
                        SessionBootstrapResult.GoLogin -> goLoginClearingBackStack()
                    }
                }
            )
        }

        composable(Route.Login.path) {
            LoginScreen(
                vm = loginVm,
                onRequestAccess = { nav.navigate(Route.RequestAccess.path) },
                onReport = { nav.navigate(Route.ReportAccess.path) },
                onLoggedIn = {
                    nav.navigate(Route.Scanner.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.RequestAccess.path) {
            RequestAccessScreen(
                vm = requestAccessVm,
                onBackToLogin = { nav.popBackStack() }
            )
        }

        composable(Route.Scanner.path) {
            ScannerScreen(
                vm = scannerVm,
                onLoteDetected = { lote -> nav.navigate(Route.Result.create(lote)) },
                onLogout = { logoutAndGoLogin() }
            )
        }

        composable(Route.Result.path) { backStack ->
            val lote = backStack.arguments?.getString("lote").orEmpty()
            val vm = remember(lote) { resultVmFactory() }

            ResultScreen(
                vm = vm,
                lote = lote,
                onReport = { nav.navigate(Route.Report.create(it)) },
                onShare = onShare,
                onGoToLogin = { logoutAndGoLogin() },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Route.Report.path) { backStack ->
            val lote = backStack.arguments?.getString("lote").orEmpty()
            ReportProblemScreen(
                vm = reportVm,
                lote = lote,
                mode = ReportMode.SCAN,
                onDone = { nav.popBackStack() }
            )
        }

        composable(Route.ReportAccess.path) {
            ReportProblemScreen(
                vm = reportVm,
                lote = "ACCESO",
                mode = ReportMode.ACCESS,
                onDone = { nav.popBackStack() }
            )
        }
    }
}
