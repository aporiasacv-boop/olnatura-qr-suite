package com.company.olnaturaqr.support.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Interpreta el Estado Operativo del lote desde Dynamics (única fuente de verdad para el banner).
 * No usa {@code qr_labels.status}.
 *
 * <pre>
 * 1) Warehouse REM  → RECHAZADO
 * 2) Warehouse RES  → RECHAZADO
 * 3) Warehouse CUARENTENA → CUARENTENA
 * 4) Otro almacén   → evaluar BatchDispositionCode (aprobado → APROBADO)
 * 5) Inconsistente / insuficiente → DESCONOCIDO
 * </pre>
 */
public final class OperationalStatusResolver {

    public static final String STATUS_APROBADO = "APROBADO";
    public static final String STATUS_CUARENTENA = "CUARENTENA";
    public static final String STATUS_RECHAZADO = "RECHAZADO";
    public static final String STATUS_DESCONOCIDO = "DESCONOCIDO";

    public static final String SOURCE_DYNAMICS = "Dynamics 365 Finance & Operations";
    public static final String RULE_WAREHOUSE_REM = "Almacén REM";
    public static final String RULE_WAREHOUSE_RES = "Almacén RES";
    public static final String RULE_WAREHOUSE_CUARENTENA = "Almacén CUARENTENA";
    public static final String RULE_BATCH_DISPOSITION = "BatchDispositionCode";
    public static final String RULE_INSUFFICIENT = "Información insuficiente";

    private OperationalStatusResolver() {}

    public record Result(
            String status,
            String ruleApplied,
            String warehouseApplied,
            String statusSource
    ) {}

    /**
     * @param inventLocationIds almacenes desde InventDim ({@code InventLocationId}); pueden ser varios
     * @param qualityWarehouseId almacén de QualityOrderHeaders ({@code WarehouseId})
     * @param batchDispositionCode ItemBatches.BatchDispositionCode
     * @param dynamicsPresent true si hubo respuesta Dynamics usable (ItemBatches encontrado)
     */
    public static Result resolve(
            Collection<String> inventLocationIds,
            String qualityWarehouseId,
            String batchDispositionCode,
            boolean dynamicsPresent
    ) {
        if (!dynamicsPresent) {
            return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT, null, SOURCE_DYNAMICS);
        }

        List<String> warehouses = collectWarehouses(inventLocationIds, qualityWarehouseId);
        if (warehouses.isEmpty() && isBlank(batchDispositionCode)) {
            return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT, null, SOURCE_DYNAMICS);
        }

        for (String wh : warehouses) {
            String norm = normalizeWarehouse(wh);
            if ("REM".equals(norm)) {
                return new Result(STATUS_RECHAZADO, RULE_WAREHOUSE_REM, wh.trim(), SOURCE_DYNAMICS);
            }
        }
        for (String wh : warehouses) {
            String norm = normalizeWarehouse(wh);
            if ("RES".equals(norm)) {
                return new Result(STATUS_RECHAZADO, RULE_WAREHOUSE_RES, wh.trim(), SOURCE_DYNAMICS);
            }
        }
        for (String wh : warehouses) {
            String norm = normalizeWarehouse(wh);
            if ("CUARENTENA".equals(norm)) {
                return new Result(STATUS_CUARENTENA, RULE_WAREHOUSE_CUARENTENA, wh.trim(), SOURCE_DYNAMICS);
            }
        }

        // Regla 4–5: otro almacén → BatchDispositionCode
        String disp = blankToNull(batchDispositionCode);
        if (disp != null) {
            String d = disp.trim().toUpperCase(Locale.ROOT);
            if (isApprovedDisposition(d)) {
                return new Result(STATUS_APROBADO, RULE_BATCH_DISPOSITION,
                        firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
            }
            if (isRejectedDisposition(d)) {
                return new Result(STATUS_RECHAZADO, RULE_BATCH_DISPOSITION,
                        firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
            }
            if (isQuarantineDisposition(d)) {
                return new Result(STATUS_CUARENTENA, RULE_BATCH_DISPOSITION,
                        firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
            }
            return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT,
                    firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
        }

        // Disposición vacía + almacén operativo conocido (p.ej. MPM) → liberado implícito (caso 3390).
        if (!warehouses.isEmpty()) {
            return new Result(STATUS_APROBADO, RULE_BATCH_DISPOSITION,
                    firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
        }

        return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT, null, SOURCE_DYNAMICS);
    }

    private static List<String> collectWarehouses(Collection<String> inventLocationIds, String qualityWarehouseId) {
        Set<String> set = new LinkedHashSet<>();
        if (inventLocationIds != null) {
            for (String id : inventLocationIds) {
                if (!isBlank(id)) {
                    set.add(id.trim());
                }
            }
        }
        if (!isBlank(qualityWarehouseId)) {
            set.add(qualityWarehouseId.trim());
        }
        return new ArrayList<>(set);
    }

    /** Normaliza almacén: REM, RES, CUARENTENA (exacto, case-insensitive). */
    static String normalizeWarehouse(String warehouse) {
        if (isBlank(warehouse)) {
            return "";
        }
        return warehouse.trim().toUpperCase(Locale.ROOT);
    }

    static boolean isApprovedDisposition(String upper) {
        return "APROBADO".equals(upper)
                || "DISPONIBLE".equals(upper)
                || "APPROVED".equals(upper)
                || "AVAILABLE".equals(upper)
                || "LIBERADO".equals(upper)
                || "RELEASED".equals(upper);
    }

    static boolean isRejectedDisposition(String upper) {
        return "RECHAZADO".equals(upper)
                || "REJECTED".equals(upper)
                || "UNAVAILABLE".equals(upper);
    }

    static boolean isQuarantineDisposition(String upper) {
        return "CUARENTENA".equals(upper)
                || "QUARANTINE".equals(upper)
                || "HOLD".equals(upper);
    }

    private static String firstWarehouseOrNull(List<String> warehouses) {
        return warehouses == null || warehouses.isEmpty() ? null : warehouses.get(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
