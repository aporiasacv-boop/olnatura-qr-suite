package com.olnatura.qr.ui.navigation

sealed class Route(val path: String) {
    data object Boot : Route("boot")
    data object Login : Route("login")
    data object RequestAccess : Route("request-access")
    data object Scanner : Route("scanner")
    data object Result : Route("result/{lote}") {
        fun create(lote: String) = "result/$lote"
    }
    data object Report : Route("report/{lote}") {
        fun create(lote: String) = "report/$lote"
    }
    /** Reporte desde login (problemas de acceso / cuenta). */
    data object ReportAccess : Route("report-access")
}
