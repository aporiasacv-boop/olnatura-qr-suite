package com.olnatura.qr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.olnatura.qr.core.session.SessionManager
import com.olnatura.qr.ui.screen.login.LoginScreen
import com.olnatura.qr.ui.screen.login.LoginViewModel
import com.olnatura.qr.ui.screen.report.ReportProblemScreen
import com.olnatura.qr.ui.screen.report.ReportProblemViewModel
import com.olnatura.qr.ui.screen.result.ResultScreen
import com.olnatura.qr.ui.screen.result.ResultViewModel
import com.olnatura.qr.ui.screen.scanner.ScannerScreen
import com.olnatura.qr.ui.screen.scanner.ScannerViewModel

@Composable
fun AppNavGraph(
    sessionManager: SessionManager,
    loginVm: LoginViewModel,
    scannerVm: ScannerViewModel,
    resultVmFactory: () -> ResultViewModel,
    reportVm: ReportProblemViewModel,
    onShare: (lote: String, status: String) -> Unit
) {
    val nav = rememberNavController()

    LaunchedEffect(Unit) {
        sessionManager.unauthorized.collect {
            nav.navigate(Route.Login.path) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = nav, startDestination = Route.Login.path) {
        composable(Route.Login.path) {
            LoginScreen(vm = loginVm) {
                nav.navigate(Route.Scanner.path) {
                    popUpTo(Route.Login.path) { inclusive = true }
                }
            }
        }

        composable(Route.Scanner.path) {
            ScannerScreen(vm = scannerVm) { lote ->
                nav.navigate(Route.Result.create(lote))
            }
        }

        composable(Route.Result.path) { backStack ->
            val lote = backStack.arguments?.getString("lote").orEmpty()
            val vm = remember(lote) { resultVmFactory() }

            ResultScreen(
                vm = vm,
                lote = lote,
                onReport = { nav.navigate(Route.Report.create(it)) },
                onShare = onShare,
                onGoToLogin = {
                    nav.navigate(Route.Login.path) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Route.Report.path) { backStack ->
            val lote = backStack.arguments?.getString("lote").orEmpty()
            ReportProblemScreen(vm = reportVm, lote = lote) { nav.popBackStack() }
        }
    }
}