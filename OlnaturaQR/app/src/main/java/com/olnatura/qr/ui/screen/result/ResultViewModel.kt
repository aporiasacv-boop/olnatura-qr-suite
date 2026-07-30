package com.olnatura.qr.ui.screen.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.model.LoteCommentResponse
import com.olnatura.qr.data.model.MeResponse
import com.olnatura.qr.data.model.QrResponse
import com.olnatura.qr.data.model.ScanEventResponse
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

data class ResultState(
    val lote: String = "",
    val gate: GateState = GateState.Checking,
    val loading: Boolean = false,
    val notFound: Boolean = false,
    val syncing: Boolean = false,
    val syncError: String? = null,
    val me: MeResponse? = null,
    val roles: Set<String> = emptySet(),
    val qr: QrResponse? = null,
    val events: List<ScanEventResponse> = emptyList(),
    val comments: List<LoteCommentResponse> = emptyList(),
    val commentsAllowed: Boolean = false,
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
    private val commentRepo: CommentRepository
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
        _state.update {
            it.copy(
                me = me,
                roles = roles,
                commentsAllowed = commentsAllowed,
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

        _state.update {
            it.copy(
                loading = false,
                events = events,
                comments = comments,
                todayCount = todayCount
            )
        }
    }

    fun syncWithDynamics() = viewModelScope.launch {
        val s = _state.value
        val lote = s.lote
        if (lote.isBlank() || s.syncing || s.loading) return@launch
        _state.update { it.copy(syncing = true, syncError = null) }
        try {
            val qr = qrRepo.syncDynamics(lote)
            _state.update {
                it.copy(
                    qr = qr,
                    syncing = false,
                    syncError = null,
                    error = null
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
    }
}