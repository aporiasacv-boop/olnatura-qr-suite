package com.olnatura.qr.ui.screen.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olnatura.qr.data.model.LoteCommentResponse
import com.olnatura.qr.ui.components.LabelValueRow
import com.olnatura.qr.ui.components.OlnTopBar
import com.olnatura.qr.ui.components.PillButton
import com.olnatura.qr.ui.components.StatusBanner
import com.olnatura.qr.ui.components.TabletContent
import com.olnatura.qr.ui.components.operationalStatusLabel
import com.olnatura.qr.ui.components.statusColors
import com.olnatura.qr.ui.theme.OlnCard
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen
import com.olnatura.qr.ui.theme.OlnTextMuted
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ResultScreen(
    vm: ResultViewModel,
    lote: String,
    onGoToLogin: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(lote) {
        vm.load(lote)
    }

    Scaffold(
        topBar = { OlnTopBar(title = "Datos de consulta", onBack = onBack) },
        containerColor = OlnCream
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
                                    syncing = state.syncing,
                                    syncError = state.syncError,
                                    isAdmin = state.isAdmin,
                                    reprintBusy = state.reprintBusy,
                                    reprintError = state.reprintError,
                                    commentsVisible = state.commentsVisible,
                                    canCreateComments = state.canCreateComments,
                                    comments = state.comments,
                                    commentDraft = state.commentDraft,
                                    commentBusy = state.commentBusy,
                                    commentError = state.commentError,
                                    onSyncDynamics = vm::syncWithDynamics,
                                    onDismissSyncError = vm::clearSyncError,
                                    onConfirmReprint = vm::confirmPhysicalReprint,
                                    onCommentDraft = vm::onCommentDraft,
                                    onSubmitComment = vm::submitComment
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
    syncing: Boolean,
    syncError: String?,
    isAdmin: Boolean,
    reprintBusy: Boolean,
    reprintError: String?,
    commentsVisible: Boolean,
    canCreateComments: Boolean,
    comments: List<LoteCommentResponse>,
    commentDraft: String,
    commentBusy: Boolean,
    commentError: String?,
    onSyncDynamics: () -> Unit,
    onDismissSyncError: () -> Unit,
    onConfirmReprint: () -> Unit,
    onCommentDraft: (String) -> Unit,
    onSubmitComment: () -> Unit
) {
    val label = qr.label
    val dynamic = qr.dynamic
    val status = dynamic?.status ?: "DESCONOCIDO"
    val lastSyncedDisplay = formatLastSyncedAt(dynamic?.lastSyncedAt)
    val (bgColor, textColor) = statusColors(status)
    val reprintRequired = label.reprintRequired

    fun str(v: String?) = v?.takeIf { it.isNotBlank() } ?: "—"
    fun dateDdMmYyyy(v: String?): String {
        val raw = v?.trim().orEmpty()
        if (raw.isEmpty()) return "—"
        if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') {
            val y = raw.substring(0, 4)
            val m = raw.substring(5, 7)
            val d = raw.substring(8, 10)
            return "$d/$m/$y"
        }
        return raw
    }

    val numberFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-MX"))
    val inventoryUnit = dynamic?.unidadInventario?.takeIf { it.isNotBlank() }
        ?: dynamic?.uom?.takeIf { it.isNotBlank() }
    val cantidadRecibidaText =
        if (dynamic?.cantidadRecibida != null) {
            val qty = numberFmt.format(dynamic.cantidadRecibida)
            if (inventoryUnit != null) "$qty $inventoryUnit" else qty
        } else {
            "—"
        }

    val loteValue = str(dynamic?.lote).ifBlank { lote }
    val nombreValue = str(dynamic?.nombre)
    val codigoValue = str(dynamic?.codigo)
    val fechaEntradaRaw = dynamic?.fechaEntrada
    val caducidadRaw = dynamic?.caducidad
    val tipoFechaQr = tipoFechaEtiqueta(label.caducidad, label.reanalisis)

    Spacer(Modifier.height(8.dp))
    StatusBanner(
        text = operationalStatusLabel(status),
        bgColor = bgColor,
        textColor = textColor,
        modifier = Modifier.fillMaxWidth()
    )
    if (reprintRequired) {
        Spacer(Modifier.height(10.dp))
        WarningCard(
            title = "Etiqueta física desactualizada",
            body = "Los datos en sistema pueden estar correctos, pero la etiqueta impresa puede seguir mostrando información anterior. Reimprime y reemplaza las físicas.",
            lines = emptyList()
        )
        if (isAdmin) {
            Spacer(Modifier.height(8.dp))
            PillButton(
                text = if (reprintBusy) "Confirmando…" else "Confirmar reimpresión física",
                onClick = onConfirmReprint,
                containerColor = OlnGreen,
                modifier = Modifier.fillMaxWidth(),
                enabled = !reprintBusy
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Un administrador debe confirmar la reimpresión.",
                fontSize = 13.sp,
                color = OlnTextMuted
            )
        }
        if (reprintError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = reprintError,
                fontSize = 13.sp,
                color = androidx.compose.ui.graphics.Color(0xFFB00020)
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelValueRow("Lote", loteValue)
            LabelValueRow("Código", codigoValue)
            LabelValueRow("Nombre", nombreValue)
            LabelValueRow("Almacén", str(dynamic?.almacen))
            FechaVencimientoRow(
                dateText = dateDdMmYyyy(caducidadRaw),
                tipoQr = tipoFechaQr
            )
            LabelValueRow("Fecha de entrada", dateDdMmYyyy(fechaEntradaRaw))
            LabelValueRow("Cantidad recibida", cantidadRecibidaText)
            run {
                val menores = label.restosCantidades
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .ifEmpty {
                        if (label.restosEnabled) {
                            listOfNotNull(label.cantidadResto?.trim()?.takeIf { it.isNotEmpty() })
                        } else {
                            emptyList()
                        }
                    }
                if (menores.isNotEmpty()) {
                    LabelValueRow("Cantidad por envase", str(label.cantidadPorEnvase))
                    val total = label.envaseTotal ?: menores.size
                    val first = total - menores.size + 1
                    menores.forEachIndexed { index, qty ->
                        LabelValueRow("Envase ${first + index}", qty)
                    }
                }
            }
            val statusNorm = status.trim().uppercase(Locale.ROOT)
            if (statusNorm == "APROBADO" || statusNorm == "PARCIAL") {
                val liberacionAt = dynamic?.fechaLiberacion?.takeIf { it.isNotBlank() }
                LabelValueRow(
                    "Fecha y hora de aprobación",
                    if (liberacionAt != null) {
                        formatInstantMexico(liberacionAt)
                    } else "—",
                    showDivider = false
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    PillButton(
        text = if (syncing) "Sincronizando…" else "Sincronizar con Dynamics",
        onClick = onSyncDynamics,
        containerColor = OlnGreen,
        modifier = Modifier.fillMaxWidth(),
        enabled = !syncing
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Última sincronización",
        fontSize = 12.sp,
        color = OlnTextMuted
    )
    Text(
        text = lastSyncedDisplay,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
    if (syncError != null) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = syncError,
            fontSize = 13.sp,
            color = androidx.compose.ui.graphics.Color(0xFFB00020)
        )
        TextButton(onClick = onDismissSyncError) {
            Text("Cerrar")
        }
    }

    if (commentsVisible) {
        Spacer(Modifier.height(22.dp))
        CommentsSection(
            comments = comments,
            canCreateComments = canCreateComments,
            commentDraft = commentDraft,
            commentBusy = commentBusy,
            commentError = commentError,
            onCommentDraft = onCommentDraft,
            onSubmitComment = onSubmitComment
        )
    }
}

@Composable
private fun FechaVencimientoRow(
    dateText: String,
    tipoQr: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Fecha de vencimiento",
                color = OlnTextMuted,
                modifier = Modifier.weight(1f)
            )
            if (!tipoQr.isNullOrBlank()) {
                Surface(
                    color = OlnCream,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        tipoQr,
                        color = OlnGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Text(dateText)
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun WarningCard(
    title: String,
    body: String,
    lines: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF0DFB8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF8A3B0A))
            Text(body, fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFF8A3B0A))
            lines.forEach { line ->
                Text("• $line", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFF8A3B0A))
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<LoteCommentResponse>,
    canCreateComments: Boolean,
    commentDraft: String,
    commentBusy: Boolean,
    commentError: String?,
    onCommentDraft: (String) -> Unit,
    onSubmitComment: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Comentarios", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

            if (comments.isEmpty()) {
                Text("Sin comentarios", fontSize = 14.sp)
            } else {
                comments.forEach { c ->
                    CommentItem(c)
                }
            }

            if (canCreateComments) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = onCommentDraft,
                    label = { Text("Escribe un comentario…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !commentBusy,
                    supportingText = { Text("${commentDraft.length}/${ResultViewModel.COMMENT_MAX}") }
                )
                if (commentError != null) {
                    Text(commentError, color = androidx.compose.ui.graphics.Color(0xFFB00020), fontSize = 13.sp)
                }
                PillButton(
                    text = if (commentBusy) "Guardando…" else "Agregar comentario",
                    onClick = onSubmitComment,
                    containerColor = OlnGreen,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !commentBusy && commentDraft.isNotBlank()
                )
            } else {
                Text(
                    "Solo usuarios autorizados por el administrador pueden agregar comentarios.",
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CommentItem(c: LoteCommentResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(formatCommentDateTime(c.createdAt), fontSize = 12.sp)
        Text(roleDisplay(c.role), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(c.displayName?.takeIf { it.isNotBlank() } ?: c.username ?: "—", fontWeight = FontWeight.SemiBold)
        Text(
            "\"${c.comment.orEmpty()}\"",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun roleDisplay(role: String?): String {
    return when (role?.trim()?.uppercase(Locale.ROOT)) {
        "INSPECCION" -> "INSPECCIÓN"
        "CALIDAD" -> "CALIDAD"
        "ALMACEN" -> "ALMACÉN"
        "PRODUCCION" -> "PRODUCCIÓN"
        "VALIDACION" -> "VALIDACIÓN"
        "ADMIN" -> "ADMINISTRADOR"
        else -> role?.uppercase(Locale.ROOT) ?: "—"
    }
}

private fun tipoFechaEtiqueta(caducidad: String?, reanalisis: String?): String? {
    fun has(v: String?): Boolean {
        val t = v?.trim().orEmpty()
        return t.isNotEmpty() && t != "—"
    }
    return when {
        has(reanalisis) -> "Reanálisis"
        has(caducidad) -> "Caducidad"
        else -> null
    }
}

private val MexicoZone: ZoneId = ZoneId.of("America/Mexico_City")
private val DateTimeMexicoFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale("es", "MX")).withZone(MexicoZone)
private val CommentMexicoFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "MX")).withZone(MexicoZone)

private fun formatCommentDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return formatInstantMexico(raw, CommentMexicoFmt)
}

private fun formatLastSyncedAt(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return formatInstantMexico(raw)
}

private fun formatInstantMexico(
    raw: String,
    formatter: DateTimeFormatter = DateTimeMexicoFmt
): String {
    val instant = parseInstant(raw)
    return if (instant != null) formatter.format(instant) else raw.take(19).replace('T', ' ')
}

private fun parseInstant(raw: String): Instant? {
    val value = raw.trim()
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {
                null
            }
        }
    }
}