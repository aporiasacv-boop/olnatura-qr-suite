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

        // GATE: SIEMPRE /me primero
        val me = try {
            authRepo.me()
        } catch (_: Exception) {
            _state.update { it.copy(loading = false, gate = GateState.Unauthorized) }
            return@launch
        }

        val roles = me.roles.toSet()
        _state.update { it.copy(me = me, roles = roles, gate = GateState.Authorized) }

        // autorizado => /qr
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
                            error = (e.message ?: "No se pudo consultar el lote").take(120)
                        )
                    }
                    return@launch
                }
            }
        }

        _state.update { it.copy(qr = qr) }

        // autorizado => /scan POST (si da 401, interceptor manda a login)
        runCatching { scanRepo.postScan(lote) }

        // autorizado => /scan GET
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
}