package com.olnatura.qr.ui.screen.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.model.MeResponse
import com.olnatura.qr.data.model.QrResponse
import com.olnatura.qr.data.model.ScanEventResponse
import com.olnatura.qr.data.repo.AuthRepository
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

    val me: MeResponse? = null,
    val roles: Set<String> = emptySet(),
    val qr: QrResponse? = null,
    val events: List<ScanEventResponse> = emptyList(),
    val todayCount: Int = 0,

    val error: String? = null
)

class ResultViewModel(
    private val authRepo: AuthRepository,
    private val qrRepo: QrRepository,
    private val scanRepo: ScanRepository
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
                notFound = false,
                qr = null,
                events = emptyList(),
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

        val roles = me.roles.toSet()
        _state.update { it.copy(me = me, roles = roles, gate = GateState.Authorized) }
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

        _state.update {
            it.copy(
                loading = false,
                events = events,
                todayCount = todayCount
            )
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
}