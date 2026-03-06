package com.company.olnaturaqr.support.qr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Optional;

/**
 * Extrae el lote de un string raw (JSON, URL, o texto plano).
 */
public final class LoteExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @param raw Contenido crudo (JSON, URL con path, o lote directo)
     * @return Lote normalizado o empty si no se pudo extraer
     */
    /** Canonical format for new QR codes. */
    private static final String PREFIX = "OLNQR:1:";

    /**
     * Extracts identifier (lote or token) from raw QR content.
     * Supports: OLNQR:1:&lt;token&gt;, JSON, URL, plain text.
     */
    public static Optional<String> extract(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String t = raw.trim();

        // Canonical: OLNQR:1:<public_token>
        if (t.startsWith(PREFIX)) {
            String token = t.substring(PREFIX.length()).trim();
            if (!token.isBlank() && token.length() <= 64) return Optional.of(token);
            return Optional.empty();
        }

        // Legacy: JSON
        Optional<String> fromJson = extractFromJson(t);
        if (fromJson.isPresent()) return fromJson;

        // Legacy: URL
        Optional<String> fromUrl = extractFromUrl(t);
        if (fromUrl.isPresent()) return fromUrl;

        // Legacy: plain lote or token
        if (t.length() >= 1 && t.length() <= 120) return Optional.of(t);
        return Optional.empty();
    }

    private static Optional<String> extractFromJson(String raw) {
        if (!raw.startsWith("{")) return Optional.empty();
        try {
            JsonNode node = MAPPER.readTree(raw);
            for (String key : new String[]{"lote", "batch", "lot", "loteId"}) {
                if (node.has(key) && !node.get(key).isNull()) {
                    String v = node.get(key).asText("").trim();
                    if (!v.isBlank()) return Optional.of(v);
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static Optional<String> extractFromUrl(String raw) {
        if (!raw.contains("/")) return Optional.empty();
        try {
            URI uri = URI.create(raw);
            String path = uri.getPath();
            if (path == null) return Optional.empty();
            String[] segments = path.split("/");
            for (int i = 0; i < segments.length - 1; i++) {
                if ("qr".equalsIgnoreCase(segments[i]) && i + 1 < segments.length) {
                    String next = segments[i + 1].trim();
                    if (!next.isBlank()) return Optional.of(next);
                }
            }
            String last = segments[segments.length - 1];
            if (last != null && !last.isBlank()) return Optional.of(last);
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
