package com.olnatura.qr.core.util

import android.net.Uri
import org.json.JSONObject

/**
 * Extrae el identificador de lote del contenido escaneado del QR.
 * Soporta: JSON con campo "lote", URL con path /qr/{lote}, texto plano.
 */
object LoteExtractor {

    /**
     * @param raw Contenido crudo del QR (texto, JSON, URL).
     * @return Lote extraído o null si no se pudo extraer.
     */
    fun extract(raw: String?): String? {
        val t = raw?.trim() ?: return null
        if (t.isBlank()) return null

        // 1. JSON: {"lote":"LOTE-001"} o {"batch":"...","lote":"LOTE-001"}
        extractFromJson(t)?.let { return it }

        // 2. URL: .../qr/LOTE-001 o .../api/v1/qr/LOTE-001
        extractFromUrl(t)?.let { return it }

        // 3. Texto plano
        return t.takeIf { it.length in 1..128 }
    }

    private fun extractFromJson(raw: String): String? {
        if (!raw.trimStart().startsWith("{")) return null
        return try {
            val obj = JSONObject(raw)
            listOf("lote", "batch", "lot", "loteId")
                .firstNotNullOfOrNull { key ->
                    obj.optString(key, "").trim().takeIf { it.isNotBlank() }
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFromUrl(raw: String): String? {
        if (!raw.contains("/")) return null
        return try {
            val uri = Uri.parse(raw)
            val segments = uri.pathSegments ?: return null
            val idxQr = segments.indexOfLast { it.equals("qr", ignoreCase = true) }
            if (idxQr >= 0 && idxQr + 1 < segments.size) {
                segments[idxQr + 1].trim().takeIf { it.isNotBlank() }
            } else {
                uri.lastPathSegment?.trim()?.takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
