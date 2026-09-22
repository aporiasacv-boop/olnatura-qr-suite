package com.olnatura.qr.ui.screen.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.repo.ProblemReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class ReportProblemViewModel(
    private val reportRepo: ProblemReportRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ReportUiState())
    val ui = _ui.asStateFlow()

    fun reset() {
        _ui.value = ReportUiState()
    }

    fun submit(mode: ReportMode, lote: String, reason: String, comment: String) {
        if (reason.isBlank()) {
            _ui.update { it.copy(error = "Selecciona un motivo") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            val kind = when (mode) {
                ReportMode.SCAN -> "SCAN"
                ReportMode.ACCESS -> "ACCESS"
            }
            val loteToSend = when (mode) {
                ReportMode.SCAN -> lote.takeIf { it.isNotBlank() && !it.equals("ACCESO", ignoreCase = true) }
                ReportMode.ACCESS -> null
            }
            reportRepo.submit(kind, loteToSend, reason, comment)
                .onSuccess {
                    _ui.update { it.copy(busy = false, success = true, error = null) }
                }
                .onFailure {
                    _ui.update {
                        it.copy(
                            busy = false,
                            error = "No se pudo enviar el reporte. Revisa tu conexión e intenta de nuevo."
                        )
                    }
                }
        }
    }
}
