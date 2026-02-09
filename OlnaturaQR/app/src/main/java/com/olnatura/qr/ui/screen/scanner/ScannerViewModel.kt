package com.olnatura.qr.ui.screen.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScannerState(
    val lastRaw: String? = null,
    val error: String? = null
)

class ScannerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScannerState())
    val state = _state.asStateFlow()

    fun consumeQr(raw: String, onLote: (String) -> Unit) {
        if (_state.value.lastRaw == raw) return
        _state.update { it.copy(lastRaw = raw, error = null) }
        val lote = extractLote(raw)
        if (lote.isNullOrBlank()) _state.update { it.copy(error = "QR inválido: no pude extraer lote") }
        else onLote(lote)
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

/**
 * - Si es URL .../qr/{lote} o .../api/v1/qr/{lote}: toma {lote}
 * - Si es texto: lo toma como lote
 */
fun extractLote(raw: String): String? {
    val t = raw.trim()
    if (t.isBlank()) return null

    runCatching {
        val uri = android.net.Uri.parse(t)
        val segments = uri.pathSegments ?: emptyList()
        val idxQr = segments.indexOfLast { it.equals("qr", ignoreCase = true) }
        if (idxQr >= 0 && idxQr + 1 < segments.size) return segments[idxQr + 1]
        val last = uri.lastPathSegment
        if (!last.isNullOrBlank()) return last
    }

    return t.takeIf { it.length in 1..128 }
}