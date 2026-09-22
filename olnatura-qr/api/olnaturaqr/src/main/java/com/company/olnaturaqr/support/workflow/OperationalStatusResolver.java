package com.company.olnaturaqr.support.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class OperationalStatusResolver {

    public static final String STATUS_APROBADO = "APROBADO";
    public static final String STATUS_CUARENTENA = "CUARENTENA";
    public static final String STATUS_RECHAZADO = "RECHAZADO";
    public static final String STATUS_PARCIAL = "PARCIAL";
    public static final String STATUS_DESCONOCIDO = "DESCONOCIDO";

    public static final String SOURCE_DYNAMICS = "Dynamics 365 Finance & Operations";
    public static final String RULE_WAREHOUSE_REM = "Almacén REM";
    public static final String RULE_WAREHOUSE_RES = "Almacén RES";
    public static final String RULE_WAREHOUSE_PARTIAL = "Almacén disponible + REM/RES";
    public static final String RULE_WAREHOUSE_CUARENTENA = "Almacén CUARENTENA";
    public static final String RULE_OPERABLE_WITHOUT_QUALITY = "Almacén operable sin QualityOrder";
    public static final String RULE_QUALITY_ORDER_OPEN = "QualityOrderStatus Open";
    public static final String RULE_QUALITY_PASS_APPROVED = "QualityOrder Pass + BatchDispositionCode";
    public static final String RULE_QUALITY_PASS_VALIDATED = "QualityOrder Pass + ValidatedDateTime";
    public static final String RULE_BATCH_DISPOSITION = "BatchDispositionCode";
    public static final String RULE_INSUFFICIENT = "Información insuficiente";

    private static final Set<String> OPERABLE_WAREHOUSES = Set.of("MEM", "MES", "MPS", "MPM");

    // Caso almacén operable + REM/RES + liberado: en Dynamics no existe PARCIAL;
    // se reporta como APROBADO. Flag conservado por si hace falta desactivar la rama.
    public static final boolean ENABLE_PARTIAL_STATE_EXPERIMENT = true;

    private OperationalStatusResolver() {}

    public record Result(
            String status,
            String ruleApplied,
            String warehouseApplied,
            String statusSource
    ) {}

    /**
     * Compatibilidad: sin QualityOrderStatus ni ValidatedDateTime.
     */
    public static Result resolve(
            Collection<String> inventLocationIds,
            String qualityWarehouseId,
            String batchDispositionCode,
            boolean dynamicsPresent
    ) {
        return resolve(inventLocationIds, qualityWarehouseId, batchDispositionCode, null, null, dynamicsPresent);
    }

    /**
     * Compatibilidad: sin ValidatedDateTime (equivalente a pasar {@code null}).
     * {@code batchDispositionCode} se ignora en la decisión (solo diagnóstico externo).
     */
    public static Result resolve(
            Collection<String> inventLocationIds,
            String qualityWarehouseId,
            String batchDispositionCode,
            String qualityOrderStatus,
            boolean dynamicsPresent
    ) {
        return resolve(
                inventLocationIds,
                qualityWarehouseId,
                batchDispositionCode,
                qualityOrderStatus,
                null,
                dynamicsPresent
        );
    }

    /**
     * Prioridad:
     * 1 (si ENABLE_PARTIAL_STATE_EXPERIMENT) operable + REM/RES + Pass + ValidatedDateTime → APROBADO
     * 2 REM / RES → RECHAZADO
     * 3 QualityOrderStatus Open → CUARENTENA
     * 4 Pass + ValidatedDateTime → APROBADO
     * 5 Almacén operable (MEM/MES/MPS/MPM) sin QualityOrder → CUARENTENA
     * else → DESCONOCIDO
     * <p>
     * {@code batchDispositionCode} no participa en la decisión.
     */
    public static Result resolve(
            Collection<String> inventLocationIds,
            String qualityWarehouseId,
            String batchDispositionCode,
            String qualityOrderStatus,
            String validatedDateTime,
            boolean dynamicsPresent
    ) {
        if (!dynamicsPresent) {
            return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT, null, SOURCE_DYNAMICS);
        }

        List<String> warehouses = collectWarehouses(inventLocationIds, qualityWarehouseId);
        boolean hasRem = warehouses.stream().anyMatch(w -> "REM".equals(normalizeWarehouse(w)));
        boolean hasRes = warehouses.stream().anyMatch(w -> "RES".equals(normalizeWarehouse(w)));
        boolean hasRejectWh = hasRem || hasRes;
        boolean hasOperableWh = warehouses.stream().anyMatch(w -> OPERABLE_WAREHOUSES.contains(normalizeWarehouse(w)));
        boolean liberated = isPassedQualityStatus(qualityOrderStatus)
                && isValidValidatedDateTime(validatedDateTime);

        if (ENABLE_PARTIAL_STATE_EXPERIMENT && hasRejectWh && hasOperableWh && liberated) {
            String applied = hasRem
                    ? firstMatchingWarehouse(warehouses, "REM")
                    : firstMatchingWarehouse(warehouses, "RES");
            return new Result(STATUS_APROBADO, RULE_WAREHOUSE_PARTIAL, applied, SOURCE_DYNAMICS);
        }

        if (hasRem) {
            return new Result(STATUS_RECHAZADO, RULE_WAREHOUSE_REM,
                    firstMatchingWarehouse(warehouses, "REM"), SOURCE_DYNAMICS);
        }
        if (hasRes) {
            return new Result(STATUS_RECHAZADO, RULE_WAREHOUSE_RES,
                    firstMatchingWarehouse(warehouses, "RES"), SOURCE_DYNAMICS);
        }

        if (isPendingQualityStatus(qualityOrderStatus)) {
            return new Result(STATUS_CUARENTENA, RULE_QUALITY_ORDER_OPEN,
                    firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
        }

        if (liberated) {
            return new Result(STATUS_APROBADO, RULE_QUALITY_PASS_VALIDATED,
                    firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
        }

        if (hasOperableWh && isBlank(qualityOrderStatus)) {
            return new Result(STATUS_CUARENTENA, RULE_OPERABLE_WITHOUT_QUALITY,
                    firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
        }

        return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT,
                firstWarehouseOrNull(warehouses), SOURCE_DYNAMICS);
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

    static String normalizeWarehouse(String warehouse) {
        if (isBlank(warehouse)) {
            return "";
        }
        return warehouse.trim().toUpperCase(Locale.ROOT);
    }

    static boolean isPendingQualityStatus(String qualityOrderStatus) {
        if (isBlank(qualityOrderStatus)) {
            return false;
        }
        String s = qualityOrderStatus.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return "OPEN".equals(s)
                || "OPENED".equals(s)
                || "PENDING".equals(s)
                || "INPROGRESS".equals(s)
                || "IN_PROGRESS".equals(s)
                || "STARTED".equals(s)
                || "DRAFT".equals(s);
    }

    static boolean isPassedQualityStatus(String qualityOrderStatus) {
        if (isBlank(qualityOrderStatus)) {
            return false;
        }
        String s = qualityOrderStatus.trim().toUpperCase(Locale.ROOT);
        return "PASS".equals(s) || "PASSED".equals(s);
    }

    static boolean isValidValidatedDateTime(String validatedDateTime) {
        if (isBlank(validatedDateTime)) {
            return false;
        }
        String trimmed = validatedDateTime.trim();
        return !trimmed.startsWith("1900-01-01");
    }

    static boolean isApprovedDispositionValue(String disposition) {
        if (isBlank(disposition)) {
            return false;
        }
        return isApprovedDisposition(disposition.trim().toUpperCase(Locale.ROOT));
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

    private static String firstMatchingWarehouse(List<String> warehouses, String code) {
        if (warehouses == null) {
            return null;
        }
        for (String wh : warehouses) {
            if (code.equals(normalizeWarehouse(wh))) {
                return wh.trim();
            }
        }
        return null;
    }

    private static String firstWarehouseOrNull(List<String> warehouses) {
        return warehouses == null || warehouses.isEmpty() ? null : warehouses.get(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
