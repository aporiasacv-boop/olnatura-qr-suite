package com.olnatura.qr.core.util

import android.net.Uri
import org.json.JSONObject

private const val PREFIX = "OLNQR:1:"

/**
 * Extrae el identificador (lote o token) del contenido escaneado del QR.
 * Soporta: OLNQR:1:&lt;token&gt;, JSON, URL con path /qr/{lote}, texto plano.
 */
object LoteExtractor {

    /**
     * @param raw Contenido crudo del QR (texto, JSON, URL).
     * @return Identificador extraído (token o lote) o null.
     */
    fun extract(raw: String?): String? {
        val t = raw?.trim() ?: return null
        if (t.isBlank()) return null

        // 1. Canonical: OLNQR:1:<public_token>
        if (t.startsWith(PREFIX)) {
            val token = t.removePrefix(PREFIX).trim()
            return token.takeIf { it.isNotBlank() && it.length <= 64 }
        }

        // 2. Legacy: JSON
        extractFromJson(t)?.let { return it }

        // 3. Legacy: URL
        extractFromUrl(t)?.let { return it }

        // 4. Legacy: texto plano
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
