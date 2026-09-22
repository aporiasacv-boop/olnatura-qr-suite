package com.company.olnaturaqr.support.label;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class EnvaseRestos {

    public static final int MAX = 4;
    private static final ObjectMapper JSON = new ObjectMapper();

    private EnvaseRestos() {}

    public static void apply(
            QrLabel q,
            List<String> cantidades,
            String cantidadPorEnvase,
            int envaseTotal
    ) {
        if (q == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Etiqueta requerida");
        }
        List<String> cleaned = normalize(cantidades);
        if (cleaned.isEmpty()) {
            q.setRestosEnabled(false);
            q.setCantidadResto(null);
            q.setRestosCantidadesJson(null);
            return;
        }
        if (cleaned.size() > MAX) {
            throw new ResponseStatusException(BAD_REQUEST, "Máximo " + MAX + " etiquetas de resto");
        }
        if (envaseTotal < cleaned.size()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "La cantidad total de envases debe ser al menos " + cleaned.size());
        }
        String stdRaw = trimToNull(cantidadPorEnvase);
        if (stdRaw == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Cantidad por envase es requerida");
        }
        BigDecimal std = parsePositive(stdRaw, "Cantidad por envase");
        for (int i = 0; i < cleaned.size(); i++) {
            String raw = cleaned.get(i);
            BigDecimal qty = parsePositive(raw, "Etiqueta de resto " + (i + 1));
            if (qty.compareTo(std) >= 0) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "La etiqueta de resto " + (i + 1) + " debe ser menor a la cantidad por envase");
            }
        }
        q.setRestosEnabled(true);
        q.setRestosCantidadesJson(writeJson(cleaned));
        q.setCantidadResto(cleaned.get(cleaned.size() - 1));
    }

    public static List<String> listOf(QrLabel q) {
        if (q == null) return List.of();
        List<String> fromJson = readJson(q.getRestosCantidadesJson());
        if (!fromJson.isEmpty()) return fromJson;
        if (q.isRestosEnabled()) {
            String legacy = trimToNull(q.getCantidadResto());
            if (legacy != null) return List.of(legacy);
        }
        return List.of();
    }

    public static boolean isCantidadMenorEnvase(QrLabel q, int envaseNum) {
        List<String> list = listOf(q);
        if (list.isEmpty() || q == null) return false;
        int total = q.getEnvaseTotal();
        int first = total - list.size() + 1;
        return envaseNum >= first && envaseNum <= total;
    }

    public static String cantidadForEnvase(QrLabel q, int envaseNum) {
        if (q == null) return "N/A";
        List<String> list = listOf(q);
        if (!list.isEmpty()) {
            int total = q.getEnvaseTotal();
            int first = total - list.size() + 1;
            if (envaseNum >= first && envaseNum <= total) {
                return list.get(envaseNum - first);
            }
        }
        String std = trimToNull(q.getCantidadPorEnvase());
        return std == null ? "N/A" : std;
    }

    public static List<String> quantities(QrLabel q) {
        if (q == null) return List.of();
        int total = Math.max(0, q.getEnvaseTotal());
        List<String> out = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            out.add(cantidadForEnvase(q, i));
        }
        return Collections.unmodifiableList(out);
    }

    public static String cantidadTotal(QrLabel q) {
        List<String> qs = quantities(q);
        if (qs.isEmpty()) return "0";
        BigDecimal sum = BigDecimal.ZERO;
        int parsed = 0;
        for (String raw : qs) {
            String t = trimToNull(raw);
            if (t == null || "N/A".equalsIgnoreCase(t)) continue;
            try {
                sum = sum.add(parsePositive(t, "cantidad"));
                parsed++;
            } catch (RuntimeException ignored) {
            }
        }
        if (parsed == 0) return String.valueOf(qs.size());
        return sum.stripTrailingZeros().toPlainString();
    }

    static List<String> normalize(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String item : raw) {
            String t = trimToNull(item);
            if (t == null) {
                throw new ResponseStatusException(BAD_REQUEST, "No se permiten cantidades menores vacías");
            }
            out.add(t);
        }
        return out;
    }

    static BigDecimal parsePositive(String raw, String field) {
        String n = raw.trim().replace(" ", "").replace(",", ".");
        int i = 0;
        StringBuilder num = new StringBuilder();
        if (i < n.length() && (n.charAt(i) == '+' || n.charAt(i) == '-')) {
            num.append(n.charAt(i));
            i++;
        }
        boolean dot = false;
        while (i < n.length()) {
            char c = n.charAt(i);
            if (c >= '0' && c <= '9') {
                num.append(c);
            } else if (c == '.' && !dot) {
                num.append(c);
                dot = true;
            } else {
                break;
            }
            i++;
        }
        if (num.length() == 0 || num.toString().equals("+") || num.toString().equals("-") || num.toString().equals(".")) {
            throw new ResponseStatusException(BAD_REQUEST, field + " debe ser numérica");
        }
        try {
            BigDecimal v = new BigDecimal(num.toString());
            if (v.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, field + " debe ser mayor a 0");
            }
            return v;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(BAD_REQUEST, field + " debe ser numérica");
        }
    }

    static String writeJson(List<String> list) {
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudieron guardar las cantidades menores");
        }
    }

    static List<String> readJson(String json) {
        String t = trimToNull(json);
        if (t == null) return List.of();
        try {
            List<String> parsed = JSON.readValue(t, new TypeReference<List<String>>() {});
            if (parsed == null || parsed.isEmpty()) return List.of();
            List<String> out = new ArrayList<>();
            for (String item : parsed) {
                String v = trimToNull(item);
                if (v != null) out.add(v);
            }
            return Collections.unmodifiableList(out);
        } catch (Exception e) {
            String legacy = trimToNull(json);
            return legacy == null ? List.of() : List.of(legacy);
        }
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
