package com.company.olnaturaqr.api.temp;

import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.support.workflow.OperationalStatusResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TEMPORAL — arma el panel de evidencia a partir de datos ya resueltos.
 * No recalcula el Estado Operativo; solo describe hechos observados.
 */
final class OperationalStatusValidationExplainer {

    private OperationalStatusValidationExplainer() {}

    static List<OperationalStatusValidationResponse.ReasonLine> explain(DynamicsLookupDto dto) {
        List<OperationalStatusValidationResponse.ReasonLine> lines = new ArrayList<>();
        List<String> warehouses = dto.warehouses() != null ? dto.warehouses() : List.of();

        boolean hasRem = warehouses.stream().anyMatch(w -> "REM".equals(norm(w)));
        boolean hasRes = warehouses.stream().anyMatch(w -> "RES".equals(norm(w)));
        boolean hasCuarentenaWh = warehouses.stream().anyMatch(w -> "CUARENTENA".equals(norm(w)));

        String qo = blankToNull(dto.qualityOrderStatus());
        String disposition = firstNonBlank(dto.batchDispositionCode(), dto.passedBatchDispositionCode());
        String rule = blankToNull(dto.operationalStatusRule());

        // Evidencia de almacenes especiales (como en los ejemplos de validación).
        lines.add(line(hasRem, hasRem ? "Warehouse REM encontrado" : "Warehouse REM"));
        lines.add(line(hasRes, hasRes ? "Warehouse RES encontrado" : "Warehouse RES"));

        for (String wh : warehouses) {
            String n = norm(wh);
            if ("REM".equals(n) || "RES".equals(n)) {
                continue;
            }
            lines.add(line(true, "Warehouse = " + wh.trim()));
        }
        if (hasCuarentenaWh && warehouses.stream().noneMatch(w -> "CUARENTENA".equalsIgnoreCase(w.trim()))) {
            lines.add(line(true, "Warehouse = CUARENTENA"));
        }

        if (qo != null) {
            lines.add(line(true, "QualityOrderStatus = " + qo));
        } else {
            lines.add(line(false, "QualityOrderStatus (sin dato)"));
        }

        if (disposition != null) {
            boolean approvedLike = isApprovedDisposition(disposition);
            lines.add(line(approvedLike, "BatchDispositionCode = " + disposition));
        } else {
            lines.add(line(false, "BatchDispositionCode (sin dato)"));
        }

        // Resaltar la regla que efectivamente aplicó el resolver.
        if (rule != null) {
            boolean highlight = true;
            if (OperationalStatusResolver.RULE_INSUFFICIENT.equals(rule)) {
                highlight = false;
            }
            lines.add(0, line(highlight, "Regla aplicada: " + rule));
        }

        return List.copyOf(lines);
    }

    private static OperationalStatusValidationResponse.ReasonLine line(boolean matched, String text) {
        return new OperationalStatusValidationResponse.ReasonLine(matched, text);
    }

    private static String norm(String warehouse) {
        if (warehouse == null || warehouse.isBlank()) {
            return "";
        }
        return warehouse.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isApprovedDisposition(String disposition) {
        String upper = disposition.trim().toUpperCase(Locale.ROOT);
        return "APROBADO".equals(upper)
                || "DISPONIBLE".equals(upper)
                || "APPROVED".equals(upper)
                || "AVAILABLE".equals(upper)
                || "LIBERADO".equals(upper)
                || "RELEASED".equals(upper);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
