package com.olnatura.qr.ui.screen.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.components.LabelValueRow
import com.olnatura.qr.ui.components.OlnTopBar
import com.olnatura.qr.ui.components.PillButton
import com.olnatura.qr.ui.components.StatusBanner
import com.olnatura.qr.ui.components.TabletContent
import com.olnatura.qr.ui.components.statusColors
import com.olnatura.qr.ui.share.SharePayload
import com.olnatura.qr.ui.theme.OlnCard
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ResultScreen(
    vm: ResultViewModel,
    lote: String,
    onReport: (String) -> Unit,
    onShare: (SharePayload) -> Unit,
    onGoToLogin: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(lote) {
        vm.load(lote)
    }

    Scaffold(
        topBar = { OlnTopBar(title = "Datos de consulta", onBack = onBack) },
        containerColor = OlnCream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Surface(
            color = OlnCream,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabletContent(maxWidth = 720.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.gate) {
                    is GateState.Checking -> {
                        if (state.loading) Text("Cargando…")
                        else if (state.error != null) ErrorContent(
                            message = state.error!!,
                            onRetry = { vm.load(lote) }
                        )
                    }
                    is GateState.Unauthorized -> {
                        UnauthorizedContent(onGoToLogin = onGoToLogin)
                    }
                    is GateState.Authorized -> {
                        when {
                            state.loading -> Text("Cargando…")
                            state.notFound -> NotFoundContent(lote = lote)
                            state.error != null -> ErrorContent(
                                message = state.error!!,
                                onRetry = { vm.load(lote) }
                            )
                            state.qr != null -> SuccessContent(
                                lote = lote,
                                qr = state.qr!!,
                                todayCount = state.todayCount,
                                onReport = onReport,
                                onShare = onShare
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun UnauthorizedContent(onGoToLogin: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pide autorización para ver el contenido")
            PillButton(
                text = "Cerrar sesión",
                onClick = onGoToLogin,
                containerColor = OlnGreen,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NotFoundContent(lote: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Lote no encontrado: $lote")
            Text("Verifica el identificador e intenta de nuevo.", modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(message)
            PillButton(text = "Reintentar", onClick = onRetry, containerColor = OlnGreen)
        }
    }
}

@Composable
private fun SuccessContent(
    lote: String,
    qr: com.olnatura.qr.data.model.QrResponse,
    todayCount: Int,
    onReport: (String) -> Unit,
    onShare: (SharePayload) -> Unit
) {
    val label = qr.label
    val dynamic = qr.dynamic
    // Platform QR status only — never QualityOrderStatus from Dynamics.
    val status = dynamic?.status ?: "DESCONOCIDO"
    val (bgColor, textColor) = statusColors(status)

    fun str(v: String?) = v?.takeIf { it.isNotBlank() } ?: "—"
    fun int(v: Int?) = v?.toString() ?: "—"
    fun dateDdMmYyyy(v: String?): String {
        val raw = v?.trim().orEmpty()
        if (raw.isEmpty()) return "—"
        // yyyy-MM-dd o yyyy-MM-ddTHH:mm:ssZ → dd/MM/yyyy
        if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') {
            val y = raw.substring(0, 4)
            val m = raw.substring(5, 7)
            val d = raw.substring(8, 10)
            return "$d/$m/$y"
        }
        return raw
    }

    val envaseText = "${int(label?.envaseNum)} / ${int(label?.envaseTotal)}"
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)
    val inventoryUnit = dynamic?.unidadInventario?.takeIf { it.isNotBlank() }
        ?: dynamic?.uom?.takeIf { it.isNotBlank() }
    val cantidadText = when {
        dynamic?.cantidadAlmacen != null -> {
            val qty = numberFmt.format(dynamic.cantidadAlmacen)
            if (inventoryUnit != null) "$qty $inventoryUnit" else qty
        }
        dynamic?.cantidad != null -> {
            val qty = numberFmt.format(dynamic.cantidad)
            if (inventoryUnit != null) "$qty $inventoryUnit" else qty
        }
        else -> "—"
    }

    val loteValue = str(label?.lote).ifBlank { lote }
    val fechaEntradaRaw = dynamic?.fechaEntrada?.takeIf { it.isNotBlank() } ?: label?.fechaEntrada
    val payload = SharePayload(
        lote = loteValue,
        status = status,
        nombre = str(label?.nombre),
        codigo = str(label?.codigo),
        ubicacion = str(dynamic?.ubicacion),
        almacen = str(dynamic?.almacen),
        inventario = cantidadText,
        statusDynamics = str(dynamic?.statusDynamics),
        fechaEntrada = dateDdMmYyyy(fechaEntradaRaw),
        caducidad = dateDdMmYyyy(label?.caducidad),
        escaneadoHoy = "V: $todayCount"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            LabelValueRow("Nombre", payload.nombre)
            LabelValueRow("Lote", payload.lote)
            LabelValueRow("Código", payload.codigo)
            LabelValueRow("Escaneado hoy", payload.escaneadoHoy)
            LabelValueRow("Ubicación", payload.ubicacion)
            LabelValueRow("Almacén", payload.almacen)
            LabelValueRow(
                label = "Inventario disponible",
                value = cantidadText,
                caption = if (cantidadText != "—") "Actualizado al momento del escaneo" else null
            )
            LabelValueRow("Estado Dynamics", payload.statusDynamics)
            LabelValueRow("Fecha de entrada", payload.fechaEntrada)
            LabelValueRow("Fecha de caducidad", payload.caducidad, showDivider = false)
        }
    }

    Spacer(Modifier.height(16.dp))
    StatusBanner(
        text = status,
        bgColor = bgColor,
        textColor = textColor,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton(
            text = "Compartir",
            onClick = { onShare(payload) },
            containerColor = OlnGreen,
            modifier = Modifier.weight(1f)
        )
        PillButton(
            text = "Reportar",
            onClick = { onReport(lote) },
            containerColor = OlnGreen,
            modifier = Modifier.weight(1f)
        )
    }
}
