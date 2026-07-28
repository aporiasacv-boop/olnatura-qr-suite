package com.olnatura.qr.ui.screen.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.olnatura.qr.ui.share.SharePayload
import com.olnatura.qr.ui.theme.OlnCard
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen
import com.olnatura.qr.ui.theme.OlnTextMuted
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
    var confirmOpen by remember { mutableStateOf(false) }
    var statusConfirmOpen by remember { mutableStateOf(false) }

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
                                syncing = state.syncing,
                                syncError = state.syncError,
                                canCorrect = state.canCorrect,
                                editing = state.editing,
                                editForm = state.editForm,
                                editBusy = state.editBusy,
                                editError = state.editError,
                                statusTargets = state.statusTargets,
                                statusTarget = state.statusTarget,
                                statusMotivo = state.statusMotivo,
                                statusCorrectBusy = state.statusCorrectBusy,
                                statusCorrectError = state.statusCorrectError,
                                commentsAllowed = state.commentsAllowed,
                                comments = state.comments,
                                commentDraft = state.commentDraft,
                                commentBusy = state.commentBusy,
                                commentError = state.commentError,
                                onSyncDynamics = vm::syncWithDynamics,
                                onDismissSyncError = vm::clearSyncError,
                                onStartEdit = vm::startEdit,
                                onCancelEdit = vm::cancelEdit,
                                onEditForm = vm::onEditForm,
                                onAskConfirm = { confirmOpen = true },
                                onStatusTarget = vm::onStatusTarget,
                                onStatusMotivo = vm::onStatusMotivo,
                                onAskStatusConfirm = { statusConfirmOpen = true },
                                onCommentDraft = vm::onCommentDraft,
                                onSubmitComment = vm::submitComment,
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

    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { if (!state.editBusy) confirmOpen = false },
            title = { Text("Confirmar modificación") },
            text = {
                Text("Se aplicará la corrección administrativa. El motivo y los valores anteriores/nuevos quedarán en auditoría.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmOpen = false
                        vm.submitCorrection()
                    },
                    enabled = !state.editBusy
                ) { Text(if (state.editBusy) "Guardando…" else "Confirmar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmOpen = false },
                    enabled = !state.editBusy
                ) { Text("Cancelar") }
            }
        )
    }

    if (statusConfirmOpen) {
        AlertDialog(
            onDismissRequest = { if (!state.statusCorrectBusy) statusConfirmOpen = false },
            title = { Text("Confirmar corrección administrativa") },
            text = {
                Text(
                    "Esto NO es una aprobación. Se corregirá el estado de plataforma (workflow interno) a ${state.statusTarget}. " +
                        "El Estado Operativo (Dynamics) no cambia. No altera historial de aprobaciones ni comentarios."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        statusConfirmOpen = false
                        vm.submitStatusCorrection()
                    },
                    enabled = !state.statusCorrectBusy
                ) { Text(if (state.statusCorrectBusy) "Guardando…" else "Confirmar corrección") }
            },
            dismissButton = {
                TextButton(
                    onClick = { statusConfirmOpen = false },
                    enabled = !state.statusCorrectBusy
                ) { Text("Cancelar") }
            }
        )
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
    syncing: Boolean,
    syncError: String?,
    canCorrect: Boolean,
    editing: Boolean,
    editForm: AdminEditForm,
    editBusy: Boolean,
    editError: String?,
    statusTargets: List<String>,
    statusTarget: String,
    statusMotivo: String,
    statusCorrectBusy: Boolean,
    statusCorrectError: String?,
    commentsAllowed: Boolean,
    comments: List<LoteCommentResponse>,
    commentDraft: String,
    commentBusy: Boolean,
    commentError: String?,
    onSyncDynamics: () -> Unit,
    onDismissSyncError: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditForm: ((AdminEditForm) -> AdminEditForm) -> Unit,
    onAskConfirm: () -> Unit,
    onStatusTarget: (String) -> Unit,
    onStatusMotivo: (String) -> Unit,
    onAskStatusConfirm: () -> Unit,
    onCommentDraft: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReport: (String) -> Unit,
    onShare: (SharePayload) -> Unit
) {
    val label = qr.label
    val dynamic = qr.dynamic
    val status = dynamic?.status ?: "DESCONOCIDO"
    // Nunca mezclar con Estado Operativo (dynamic.status / banner).
    val platformStatus = dynamic?.platformStatus?.takeIf { it.isNotBlank() } ?: "CUARENTENA"
    val statusRule = dynamic?.operationalStatusRule?.takeIf { it.isNotBlank() }
    val statusSource = dynamic?.statusSource?.takeIf { it.isNotBlank() }
        ?: "Dynamics 365 Finance & Operations"
    val lastSyncedDisplay = formatLastSyncedAt(dynamic?.lastSyncedAt)
    val (bgColor, textColor) = statusColors(status)

    fun str(v: String?) = v?.takeIf { it.isNotBlank() } ?: "—"
    fun int(v: Int?) = v?.toString() ?: "—"
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
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canCorrect) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!editing) {
                        PillButton(
                            text = "Editar (Administrador)",
                            onClick = onStartEdit,
                            containerColor = OlnGreen,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        PillButton(
                            text = "Cancelar",
                            onClick = onCancelEdit,
                            containerColor = OlnGreen,
                            modifier = Modifier.weight(1f),
                            enabled = !editBusy
                        )
                        PillButton(
                            text = "Guardar corrección",
                            onClick = onAskConfirm,
                            containerColor = OlnGreen,
                            modifier = Modifier.weight(1f),
                            enabled = !editBusy && editForm.motivo.isNotBlank()
                        )
                    }
                }
            }

            if (editing) {
                AdminEditFields(editForm = editForm, editBusy = editBusy, onEditForm = onEditForm)
                if (editError != null) {
                    Text(editError, color = androidx.compose.ui.graphics.Color(0xFFB00020), fontSize = 13.sp)
                }
            } else {
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
                LabelValueRow(
                    "Cantidad por envase",
                    label?.cantidadPorEnvase?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
                        raw.toDoubleOrNull()?.let { numberFmt.format(it) } ?: raw
                    } ?: "—"
                )
                LabelValueRow("Estado Dynamics", payload.statusDynamics)
                LabelValueRow("Fecha de entrada", payload.fechaEntrada)
                LabelValueRow("Fecha de caducidad", payload.caducidad, showDivider = false)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    StatusBanner(
        text = operationalStatusLabel(status),
        bgColor = bgColor,
        textColor = textColor,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    PillButton(
        text = if (syncing) "Sincronizando…" else "Sincronizar con Dynamics",
        onClick = onSyncDynamics,
        containerColor = OlnGreen,
        modifier = Modifier.fillMaxWidth(),
        enabled = !syncing && !editBusy && !statusCorrectBusy
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
    Text(
        text = "Fuente: $statusSource",
        fontSize = 12.sp,
        color = OlnTextMuted
    )
    if (statusRule != null) {
        Text(
            text = "Regla aplicada: $statusRule",
            fontSize = 12.sp,
            color = OlnTextMuted
        )
    }
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

    if (canCorrect && statusTargets.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        AdminStatusCorrectionCard(
            currentStatus = platformStatus,
            statusTargets = statusTargets,
            statusTarget = statusTarget,
            statusMotivo = statusMotivo,
            statusCorrectBusy = statusCorrectBusy,
            statusCorrectError = statusCorrectError,
            onStatusTarget = onStatusTarget,
            onStatusMotivo = onStatusMotivo,
            onAskStatusConfirm = onAskStatusConfirm
        )
    }

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

    if (commentsAllowed) {
        Spacer(Modifier.height(22.dp))
        CommentsSection(
            comments = comments,
            commentDraft = commentDraft,
            commentBusy = commentBusy,
            commentError = commentError,
            onCommentDraft = onCommentDraft,
            onSubmitComment = onSubmitComment
        )
    }
}

@Composable
private fun AdminStatusCorrectionCard(
    currentStatus: String,
    statusTargets: List<String>,
    statusTarget: String,
    statusMotivo: String,
    statusCorrectBusy: Boolean,
    statusCorrectError: String?,
    onStatusTarget: (String) -> Unit,
    onStatusMotivo: (String) -> Unit,
    onAskStatusConfirm: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OlnCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Corrección Administrativa", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text("Estado de plataforma actual: $currentStatus", fontWeight = FontWeight.Medium)
            Text("Estado de plataforma destino", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                statusTargets.forEach { t ->
                    PillButton(
                        text = "→ $t",
                        onClick = { onStatusTarget(t) },
                        containerColor = if (statusTarget == t) OlnGreen else OlnGreen.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f),
                        enabled = !statusCorrectBusy
                    )
                }
            }
            OutlinedTextField(
                value = statusMotivo,
                onValueChange = onStatusMotivo,
                label = { Text("Motivo *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !statusCorrectBusy,
                shape = RoundedCornerShape(12.dp)
            )
            if (statusCorrectError != null) {
                Text(statusCorrectError, color = androidx.compose.ui.graphics.Color(0xFFB00020), fontSize = 13.sp)
            }
            PillButton(
                text = if (statusCorrectBusy) "Guardando…" else "Aplicar corrección de plataforma",
                onClick = onAskStatusConfirm,
                containerColor = OlnGreen,
                modifier = Modifier.fillMaxWidth(),
                enabled = !statusCorrectBusy && statusTarget.isNotBlank() && statusMotivo.isNotBlank()
            )
        }
    }
}

@Composable
private fun AdminEditFields(
    editForm: AdminEditForm,
    editBusy: Boolean,
    onEditForm: ((AdminEditForm) -> AdminEditForm) -> Unit
) {
    AdminEditField("Tipo material", editForm.tipoMaterial, editBusy) { v -> onEditForm { it.copy(tipoMaterial = v) } }
    AdminEditField("Nombre", editForm.nombre, editBusy) { v -> onEditForm { it.copy(nombre = v) } }
    AdminEditField("Código", editForm.codigo, editBusy) { v -> onEditForm { it.copy(codigo = v) } }
    AdminEditField("Fecha entrada (dd/MM/yyyy)", editForm.fechaEntrada, editBusy) { v -> onEditForm { it.copy(fechaEntrada = v) } }
    AdminEditField("Caducidad", editForm.caducidad, editBusy) { v -> onEditForm { it.copy(caducidad = v) } }
    AdminEditField("Reanálisis", editForm.reanalisis, editBusy) { v -> onEditForm { it.copy(reanalisis = v) } }
    AdminEditField("Envase núm.", editForm.envaseNum, editBusy) { v -> onEditForm { it.copy(envaseNum = v) } }
    AdminEditField("Envases total", editForm.envaseTotal, editBusy) { v -> onEditForm { it.copy(envaseTotal = v) } }
    AdminEditField("Cantidad por envase", editForm.cantidadPorEnvase, editBusy) { v -> onEditForm { it.copy(cantidadPorEnvase = v) } }
    OutlinedTextField(
        value = editForm.motivo,
        onValueChange = { v -> onEditForm { it.copy(motivo = v.take(500)) } },
        label = { Text("Motivo de la modificación *") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
        enabled = !editBusy,
        shape = RoundedCornerShape(12.dp)
    )
    Text("Inventario/unidad Dynamics son solo consulta.", fontSize = 12.sp)
}

@Composable
private fun AdminEditField(
    label: String,
    value: String,
    enabledBusy: Boolean,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !enabledBusy,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun CommentsSection(
    comments: List<LoteCommentResponse>,
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
            Text(
                "Bitácora operativa. No se editan ni eliminan. Máx. ${ResultViewModel.COMMENT_MAX} caracteres.",
                fontSize = 13.sp
            )

            if (comments.isEmpty()) {
                Text("Sin comentarios en este lote.", fontSize = 14.sp)
            } else {
                comments.forEach { c ->
                    CommentItem(c)
                }
            }

            OutlinedTextField(
                value = commentDraft,
                onValueChange = onCommentDraft,
                label = { Text("Nuevo comentario") },
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
        "ADMIN" -> "ADMINISTRADOR"
        else -> role?.uppercase(Locale.ROOT) ?: "—"
    }
}

private fun formatCommentDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        val odt = OffsetDateTime.parse(raw)
        odt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "MX")))
    } catch (_: Exception) {
        raw.take(16).replace('T', ' ')
    }
}

/** Formato: 24/07/2026 14:36:18 */
private fun formatLastSyncedAt(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        val odt = OffsetDateTime.parse(raw)
        odt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale("es", "MX")))
    } catch (_: Exception) {
        try {
            val instant = java.time.Instant.parse(raw)
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale("es", "MX"))
                .withZone(java.time.ZoneId.systemDefault())
                .format(instant)
        } catch (_: Exception) {
            raw.take(19).replace('T', ' ')
        }
    }
}
