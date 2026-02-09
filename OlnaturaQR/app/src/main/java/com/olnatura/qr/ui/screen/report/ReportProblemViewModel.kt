package com.olnatura.qr.ui.screen.report

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReportState(
    val motivo: String = "",
    val comentario: String = ""
)

class ReportProblemViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReportState())
    val state = _state.asStateFlow()

    fun setMotivo(v: String) = _state.update { it.copy(motivo = v) }
    fun setComentario(v: String) = _state.update { it.copy(comentario = v) }
}