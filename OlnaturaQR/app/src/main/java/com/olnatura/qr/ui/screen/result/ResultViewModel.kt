package com.olnatura.qr.ui.screen.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.model.AdminCorrectLabelRequest
import com.olnatura.qr.data.model.LoteCommentResponse
import com.olnatura.qr.data.model.MeResponse
import com.olnatura.qr.data.model.QrResponse
import com.olnatura.qr.data.model.ScanEventResponse
import com.olnatura.qr.data.repo.AdminLotRepository
import com.olnatura.qr.data.repo.AuthRepository
import com.olnatura.qr.data.repo.CommentRepository
import com.olnatura.qr.data.repo.QrRepository
import com.olnatura.qr.data.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class GateState {
    data object Checking : GateState()
    data object Unauthorized : GateState()
    data object Authorized : GateState()
}

data class AdminEditForm(
    val tipoMaterial: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val fechaEntrada: String = "",
    val caducidad: String = "",
    val reanalisis: String = "",
    val envaseNum: String = "",
    val envaseTotal: String = "",
    val cantidadPorEnvase: String = "",
    val motivo: String = ""
)

data class ResultState(
    val lote: String = "",
    val gate: GateState = GateState.Checking,
    val loading: Boolean = false,
    val notFound: Boolean = false,
    /** Sync manual en curso; no vacía [qr] si falla. */
    val syncing: Boolean = false,
    val syncError: String? = null,

    val me: MeResponse? = null,
    val roles: Set<String> = emptySet(),
    val qr: QrResponse? = null,
    val events: List<ScanEventResponse> = emptyList(),
    val comments: List<LoteCommentResponse> = emptyList(),
    val commentsAllowed: Boolean = false,
    val canCorrect: Boolean = false,
    val editing: Boolean = false,
    val editForm: AdminEditForm = AdminEditForm(),
    val editBusy: Boolean = false,
    val editError: String? = null,
    val statusTargets: List<String> = emptyList(),
    val statusTarget: String = "",
    val statusMotivo: String = "",
    val statusCorrectBusy: Boolean = false,
    val statusCorrectError: String? = null,
    val commentDraft: String = "",
    val commentBusy: Boolean = false,
    val commentError: String? = null,
    val todayCount: Int = 0,

    val error: String? = null
)

class ResultViewModel(
    private val authRepo: AuthRepository,
    private val qrRepo: QrRepository,
    private val scanRepo: ScanRepository,
    private val commentRepo: CommentRepository,
    private val adminLotRepo: AdminLotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultState())
    val state = _state.asStateFlow()

    fun load(lote: String) = viewModelScope.launch {
        _state.update {
            it.copy(
                lote = lote,
                gate = GateState.Checking,
                loading = true,
                error = null,
                syncError = null,
                syncing = false,
                notFound = false,
                qr = null,
                events = emptyList(),
                comments = emptyList(),
                commentDraft = "",
                commentError = null,
                editing = false,
                editError = null,
                statusTarget = "",
                statusMotivo = "",
                statusCorrectError = null,
                todayCount = 0
            )
        }
        val me = try {
            authRepo.me()
        } catch (e: Exception) {
            val http = e as? HttpException
            when (http?.code()) {
                401, 403 -> _state.update { it.copy(loading = false, gate = GateState.Unauthorized) }
                else -> _state.update {
                    it.copy(
                        loading = false,
                        gate = GateState.Authorized,
                        error = connectionMessage(e)
                    )
                }
            }
            return@launch
        }

        val roles = me.roles.map { it.uppercase() }.toSet()
        val commentsAllowed = roles.any { it in COMMENT_ROLES }
        val canCorrect = roles.contains("ADMIN")
        _state.update {
            it.copy(
                me = me,
                roles = roles,
                commentsAllowed = commentsAllowed,
                canCorrect = canCorrect,
                gate = GateState.Authorized
            )
        }
        val qr = try {
            qrRepo.getQr(lote)
        } catch (e: Exception) {
            val http = e as? HttpException
            when (http?.code()) {
                401, 403 -> {
                    _state.update { it.copy(loading = false, gate = GateState.Unauthorized) }
                    return@launch
                }
                404 -> {
                    _state.update { it.copy(loading = false, notFound = true) }
                    return@launch
                }
                else -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            error = connectionMessage(e)
                        )
                    }
                    return@launch
                }
            }
        }

        _state.update { it.copy(qr = qr) }

        runCatching { scanRepo.postScan(lote) }
        val events = runCatching { scanRepo.history(lote) }.getOrDefault(emptyList())
        val todayCount = countToday(events)
        val comments = if (commentsAllowed) {
            runCatching { commentRepo.list(lote) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        // Solo platformStatus (qr_labels.status). Nunca usar dynamic.status (Estado Operativo Dynamics).
        val platformStatus = (qr.dynamic?.platformStatus ?: "CUARENTENA").trim().uppercase()
        val statusTargets = if (canCorrect) statusTargetsFor(platformStatus) else emptyList()

        _state.update {
            it.copy(
                loading = false,
                events = events,
                comments = comments,
                todayCount = todayCount,
                statusTargets = statusTargets,
                statusTarget = "",
                statusMotivo = ""
            )
        }
    }

    /**
     * Sincronizar con Dynamics: nueva lectura OData.
     * Conserva la información anterior si Dynamics no responde.
     * No modifica estados ni escribe en el ERP.
     */
    fun syncWithDynamics() = viewModelScope.launch {
        val s = _state.value
        val lote = s.lote
        if (lote.isBlank() || s.syncing || s.loading) return@launch
        _state.update { it.copy(syncing = true, syncError = null) }
        try {
            val qr = qrRepo.syncDynamics(lote)
            val platformStatus = (qr.dynamic?.platformStatus ?: "CUARENTENA").trim().uppercase()
            val statusTargets = if (s.canCorrect) statusTargetsFor(platformStatus) else emptyList()
            _state.update {
                it.copy(
                    qr = qr,
                    syncing = false,
                    syncError = null,
                    error = null,
                    statusTargets = statusTargets,
                    statusTarget = "",
                    statusMotivo = ""
                )
            }
        } catch (e: Exception) {
            val http = e as? HttpException
            when (http?.code()) {
                401, 403 -> {
                    _state.update { it.copy(syncing = false, gate = GateState.Unauthorized) }
                    return@launch
                }
                else -> {
                    val msg = when {
                        http?.code() == 502 || http?.code() == 504 ->
                            "No fue posible sincronizar con Dynamics. Se conservó la información anterior."
                        else ->
                            "No fue posible sincronizar. Se conservó la información anterior."
                    }
                    // Conservar qr previo.
                    _state.update { it.copy(syncing = false, syncError = msg) }
                }
            }
        }
    }

    fun clearSyncError() {
        _state.update { it.copy(syncError = null) }
    }

    fun onCommentDraft(value: String) {
        _state.update { it.copy(commentDraft = value.take(COMMENT_MAX), commentError = null) }
    }

    fun submitComment() = viewModelScope.launch {
        val s = _state.value
        val text = s.commentDraft.trim()
        if (!s.commentsAllowed || text.isEmpty() || s.commentBusy || s.lote.isBlank()) return@launch
        if (text.length > COMMENT_MAX) {
            _state.update { it.copy(commentError = "Máximo $COMMENT_MAX caracteres.") }
            return@launch
        }
        _state.update { it.copy(commentBusy = true, commentError = null) }
        try {
            val created = commentRepo.add(s.lote, text)
            _state.update {
                it.copy(
                    comments = it.comments + created,
                    commentDraft = "",
                    commentBusy = false
                )
            }
        } catch (e: Exception) {
            val http = e as? HttpException
            val msg = when (http?.code()) {
                403 -> "Tu rol no puede agregar comentarios."
                401 -> "Sesión expirada. Vuelve a iniciar sesión."
                else -> (e.message ?: "No se pudo registrar el comentario").take(160)
            }
            _state.update { it.copy(commentBusy = false, commentError = msg) }
        }
    }

    fun startEdit() {
        val qr = _state.value.qr ?: return
        if (!_state.value.canCorrect) return
        val label = qr.label
        _state.update {
            it.copy(
                editing = true,
                editError = null,
                editForm = AdminEditForm(
                    tipoMaterial = label?.tipoMaterial.orEmpty(),
                    nombre = label?.nombre.orEmpty(),
                    codigo = label?.codigo.orEmpty(),
                    fechaEntrada = toDisplayDate(label?.fechaEntrada),
                    caducidad = toDisplayDate(label?.caducidad),
                    reanalisis = toDisplayDate(label?.reanalisis),
                    envaseNum = label?.envaseNum?.toString().orEmpty(),
                    envaseTotal = label?.envaseTotal?.toString().orEmpty(),
                    cantidadPorEnvase = label?.cantidadPorEnvase.orEmpty(),
                    motivo = ""
                )
            )
        }
    }

    fun cancelEdit() {
        _state.update { it.copy(editing = false, editError = null) }
    }

    fun onEditForm(update: (AdminEditForm) -> AdminEditForm) {
        _state.update { it.copy(editForm = update(it.editForm), editError = null) }
    }

    fun submitCorrection() = viewModelScope.launch {
        val s = _state.value
        if (!s.canCorrect || !s.editing || s.editBusy || s.lote.isBlank()) return@launch
        val motivo = s.editForm.motivo.trim()
        if (motivo.isEmpty()) {
            _state.update { it.copy(editError = "El motivo de la modificación es obligatorio.") }
            return@launch
        }
        _state.update { it.copy(editBusy = true, editError = null) }
        try {
            val f = s.editForm
            adminLotRepo.correct(
                s.lote,
                AdminCorrectLabelRequest(
                    motivo = motivo,
                    tipoMaterial = f.tipoMaterial.trim(),
                    nombre = f.nombre.trim(),
                    codigo = f.codigo.trim(),
                    fechaEntrada = f.fechaEntrada.trim(),
                    caducidad = f.caducidad.trim(),
                    reanalisis = f.reanalisis.trim(),
                    envaseNum = f.envaseNum.trim().toIntOrNull(),
                    envaseTotal = f.envaseTotal.trim().toIntOrNull(),
                    cantidadPorEnvase = f.cantidadPorEnvase
                )
            )
            _state.update { it.copy(editBusy = false, editing = false) }
            load(s.lote)
        } catch (e: Exception) {
            val http = e as? HttpException
            val msg = when (http?.code()) {
                403 -> "Solo el Administrador puede corregir."
                401 -> "Sesión expirada. Vuelve a iniciar sesión."
                else -> (e.message ?: "No se pudo aplicar la corrección").take(180)
            }
            _state.update { it.copy(editBusy = false, editError = msg) }
        }
    }

    fun onStatusTarget(value: String) {
        _state.update { it.copy(statusTarget = value, statusCorrectError = null) }
    }

    fun onStatusMotivo(value: String) {
        _state.update { it.copy(statusMotivo = value.take(500), statusCorrectError = null) }
    }

    fun submitStatusCorrection() = viewModelScope.launch {
        val s = _state.value
        if (!s.canCorrect || s.statusCorrectBusy || s.lote.isBlank()) return@launch
        val target = s.statusTarget.trim().uppercase()
        val motivo = s.statusMotivo.trim()
        if (target.isEmpty()) {
            _state.update { it.copy(statusCorrectError = "Selecciona el estado destino.") }
            return@launch
        }
        if (motivo.isEmpty()) {
            _state.update { it.copy(statusCorrectError = "El motivo de la modificación es obligatorio.") }
            return@launch
        }
        if (target !in s.statusTargets) {
            _state.update { it.copy(statusCorrectError = "Transición no permitida.") }
            return@launch
        }
        _state.update { it.copy(statusCorrectBusy = true, statusCorrectError = null) }
        try {
            adminLotRepo.correctStatus(s.lote, target, motivo)
            _state.update { it.copy(statusCorrectBusy = false, statusTarget = "", statusMotivo = "") }
            load(s.lote)
        } catch (e: Exception) {
            val http = e as? HttpException
            val msg = when (http?.code()) {
                403 -> "Solo el Administrador puede corregir el estado de plataforma."
                401 -> "Sesión expirada. Vuelve a iniciar sesión."
                else -> (e.message ?: "No se pudo corregir el estado de plataforma").take(180)
            }
            _state.update { it.copy(statusCorrectBusy = false, statusCorrectError = msg) }
        }
    }

    private fun countToday(events: List<ScanEventResponse>): Int {
        val today = java.time.LocalDate.now().toString()
        return events.count { (it.createdAt ?: "").startsWith(today) }
    }

    private fun connectionMessage(e: Throwable): String {
        return when (e) {
            is UnknownHostException -> "No se pudo conectar al servidor (host no encontrado). Revisa la IP/base URL."
            is SocketTimeoutException -> "Tiempo de espera agotado al conectar con el servidor."
            is IOException -> "Error de red al conectar con el servidor. Verifica red e IP."
            is HttpException -> "Error HTTP ${e.code()} al consultar el servidor."
            else -> (e.message ?: "No se pudo consultar el lote").take(160)
        }
    }

    companion object {
        private val COMMENT_ROLES = setOf("ADMIN", "ALMACEN", "CALIDAD", "INSPECCION")
        const val COMMENT_MAX = 200

        fun statusTargetsFor(current: String): List<String> {
            return when (current.trim().uppercase()) {
                "CUARENTENA" -> listOf("APROBADO")
                "APROBADO", "RECHAZADO" -> listOf("CUARENTENA")
                else -> emptyList()
            }
        }

        fun toDisplayDate(raw: String?): String {
            val s = raw?.trim().orEmpty()
            if (s.length >= 10 && s[4] == '-' && s[7] == '-') {
                return "${s.substring(8, 10)}/${s.substring(5, 7)}/${s.substring(0, 4)}"
            }
            return s
        }
    }
}
