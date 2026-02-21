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
import com.olnatura.qr.ui.components.statusColors
import com.olnatura.qr.ui.theme.OlnCard
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen

@Composable
fun ResultScreen(
    vm: ResultViewModel,
    lote: String,
    onReport: (String) -> Unit,
    onShare: (String, String) -> Unit,
    onGoToLogin: () -> Unit
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(lote) {
        vm.load(lote)
    }

    Scaffold(
        topBar = { OlnTopBar(title = "Materia prima verificada", onBack = null) },
        containerColor = OlnCream
    ) { padding ->
        Surface(
            color = OlnCream,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.gate) {
                    is GateState.Checking -> {
                        if (state.loading) {
                            Text("Cargando…")
                        }
                    }
                    is GateState.Unauthorized -> {
                        UnauthorizedContent(onGoToLogin = onGoToLogin)
                    }
                    is GateState.Authorized -> {
                        when {
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
                text = "Ir a inicio de sesión",
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
    onShare: (String, String) -> Unit
) {
    val label = qr.label
    val dynamic = qr.dynamic
    val status = dynamic?.status ?: "DESCONOCIDO"
    val (bgColor, textColor) = statusColors(status)

    fun str(v: String?) = v?.takeIf { it.isNotBlank() } ?: "—"
    fun int(v: Int?) = v?.toString() ?: "—"

    val envaseText = "${int(label?.envaseNum)} / ${int(label?.envaseTotal)}"
    val cantidadUom = if (!str(dynamic?.uom).equals("—")) {
        "${dynamic?.cantidad ?: "—"} ${str(dynamic?.uom)}"
    } else str(dynamic?.cantidad?.toString())

    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            LabelValueRow("Nombre", str(label?.nombre))
            LabelValueRow("Lote", str(label?.lote).ifBlank { lote })
            LabelValueRow("Código", str(label?.codigo))
            LabelValueRow("Escaneado hoy", "V: $todayCount")
            LabelValueRow("Ubicación", str(dynamic?.ubicacion))
            LabelValueRow("Existencia", cantidadUom)
            LabelValueRow("Fecha de entrada", str(label?.fechaEntrada))
            LabelValueRow("Fecha de caducidad", str(label?.caducidad), showDivider = false)
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
            onClick = { onShare(lote, status) },
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
