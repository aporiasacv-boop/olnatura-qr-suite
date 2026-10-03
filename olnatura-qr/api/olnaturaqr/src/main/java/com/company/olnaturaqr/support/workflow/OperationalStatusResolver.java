package com.company.olnaturaqr.support.workflow;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
    public static final String RULE_WAREHOUSE_REJECT = "Existencia solo en almacén de rechazo";
    public static final String RULE_LOCATION_REJECT = "Existencia solo en ubicación Rechazo";
    public static final String RULE_WAREHOUSE_PARTIAL = "Almacén disponible + REM/RES";
    public static final String RULE_WAREHOUSE_CUARENTENA = "Almacén CUARENTENA";
    public static final String RULE_OPERABLE_WITHOUT_QUALITY = "Almacén operable sin QualityOrder";
    public static final String RULE_QUALITY_ORDER_OPEN = "QualityOrderStatus Open";
    public static final String RULE_QUALITY_ORDER_FAIL = "QualityOrderStatus Fail";
    public static final String RULE_EXPIRED = "Lote caducado";
    public static final String RULE_QUALITY_PASS_APPROVED = "QualityOrder Pass + BatchDispositionCode";
    public static final String RULE_QUALITY_PASS_VALIDATED = "QualityOrder Pass + ValidatedDateTime";
    public static final String RULE_BATCH_DISPOSITION = "BatchDispositionCode";
    public static final String RULE_INSUFFICIENT = "Información insuficiente";

    private static final Set<String> OPERABLE_WAREHOUSES = Set.of("MEM", "MES", "MPS", "MPM");
    private static final Set<String> REJECT_WAREHOUSES = Set.of("REM", "RES", "REM-D", "RES-D");

    // Caso existencia en almacén operable + REM/RES + liberado: en Dynamics no existe PARCIAL;
    // se reporta como APROBADO. Flag conservado por si hace falta desactivar la rama.
    public static final boolean ENABLE_PARTIAL_STATE_EXPERIMENT = true;

    // Un lote caducado con existencia no se puede usar aunque su orden de calidad diga Pass.
    // APAGADA hasta que Calidad confirme cómo queda el reanálisis en Dynamics: si el lote reanalizado
    // conserva su BatchExpirationDate vieja, esta regla lo marcaría RECHAZADO aunque esté liberado.
    public static final boolean ENABLE_EXPIRED_AS_REJECTED = false;

    private OperationalStatusResolver() {}

    public record Result(
            String status,
            String ruleApplied,
            String warehouseApplied,
            String statusSource
    ) {}

    /**
     * Existencia actual del lote en un almacén y ubicación. Solo cuentan las cantidades distintas de cero:
     * que Dynamics tenga la combinación lote + almacén no significa que hoy haya inventario ahí.
     */
    public record StockLine(String warehouseId, String locationId, double quantity) {}

    /**
     * Prioridad (gana la primera que aplica):
     * 1 Existencia solo en REM/RES/REM-D/RES-D o en ubicación Rechazo → RECHAZADO
     * 2 QualityOrderStatus Fail → RECHAZADO
     * 3 (si ENABLE_EXPIRED_AS_REJECTED) caducidad anterior a {@code today} → RECHAZADO
     * 4 Existencia en almacén operable y en rechazo a la vez: liberado → APROBADO
     *   (si ENABLE_PARTIAL_STATE_EXPERIMENT); si no → RECHAZADO
     * 5 QualityOrderStatus Open → CUARENTENA
     * 6 Pass + ValidatedDateTime → APROBADO
     * 7 Existencia en almacén operable sin QualityOrder → CUARENTENA
     * else → DESCONOCIDO
     * <p>
     * {@code qualityOrderStatus} y {@code validatedDateTime} deben ser los de la orden de calidad más reciente
     * del mismo artículo + lote. El almacén de la orden de calidad no cuenta como existencia.
     */
    public static Result resolve(
            List<StockLine> stock,
            String qualityOrderStatus,
            String validatedDateTime,
            String batchExpirationDate,
            LocalDate today,
            boolean dynamicsPresent
    ) {
        if (!dynamicsPresent) {
            return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT, null, SOURCE_DYNAMICS);
        }

        List<StockLine> withStock = new ArrayList<>();
        if (stock != null) {
            for (StockLine line : stock) {
                if (line != null && Math.abs(line.quantity()) > 1e-9 && !isBlank(line.warehouseId())) {
                    withStock.add(line);
                }
            }
        }

        String rejectWarehouse = null;
        String rejectLocationWarehouse = null;
        String usableWarehouse = null;
        for (StockLine line : withStock) {
            String wh = normalizeWarehouse(line.warehouseId());
            if (REJECT_WAREHOUSES.contains(wh)) {
                if (rejectWarehouse == null) {
                    rejectWarehouse = line.warehouseId().trim();
                }
            } else if (OPERABLE_WAREHOUSES.contains(wh)) {
                if (isRejectLocation(line.locationId())) {
                    if (rejectLocationWarehouse == null) {
                        rejectLocationWarehouse = line.warehouseId().trim();
                    }
                } else if (usableWarehouse == null) {
                    usableWarehouse = line.warehouseId().trim();
                }
            }
        }
        boolean hasReject = rejectWarehouse != null || rejectLocationWarehouse != null;
        boolean hasUsable = usableWarehouse != null;
        boolean liberated = isPassedQualityStatus(qualityOrderStatus)
                && isValidValidatedDateTime(validatedDateTime);
        String firstWarehouse = withStock.isEmpty() ? null : withStock.get(0).warehouseId().trim();

        if (hasReject && !hasUsable) {
            if (rejectWarehouse != null) {
                return new Result(STATUS_RECHAZADO, rejectRule(rejectWarehouse), rejectWarehouse, SOURCE_DYNAMICS);
            }
            return new Result(STATUS_RECHAZADO, RULE_LOCATION_REJECT, rejectLocationWarehouse, SOURCE_DYNAMICS);
        }

        if (isFailedQualityStatus(qualityOrderStatus)) {
            return new Result(STATUS_RECHAZADO, RULE_QUALITY_ORDER_FAIL, firstWarehouse, SOURCE_DYNAMICS);
        }

        if (ENABLE_EXPIRED_AS_REJECTED && isExpired(batchExpirationDate, today)) {
            return new Result(STATUS_RECHAZADO, RULE_EXPIRED, firstWarehouse, SOURCE_DYNAMICS);
        }

        if (hasReject) {
            String applied = rejectWarehouse != null ? rejectWarehouse : rejectLocationWarehouse;
            if (ENABLE_PARTIAL_STATE_EXPERIMENT && liberated) {
                return new Result(STATUS_APROBADO, RULE_WAREHOUSE_PARTIAL, applied, SOURCE_DYNAMICS);
            }
            return new Result(STATUS_RECHAZADO,
                    rejectWarehouse != null ? rejectRule(rejectWarehouse) : RULE_LOCATION_REJECT,
                    applied, SOURCE_DYNAMICS);
        }

        if (isPendingQualityStatus(qualityOrderStatus)) {
            return new Result(STATUS_CUARENTENA, RULE_QUALITY_ORDER_OPEN, firstWarehouse, SOURCE_DYNAMICS);
        }

        if (liberated) {
            return new Result(STATUS_APROBADO, RULE_QUALITY_PASS_VALIDATED, firstWarehouse, SOURCE_DYNAMICS);
        }

        if (hasUsable && isBlank(qualityOrderStatus)) {
            return new Result(STATUS_CUARENTENA, RULE_OPERABLE_WITHOUT_QUALITY, usableWarehouse, SOURCE_DYNAMICS);
        }

        return new Result(STATUS_DESCONOCIDO, RULE_INSUFFICIENT, firstWarehouse, SOURCE_DYNAMICS);
    }

    /** Almacenes con existencia, sin repetir y en el orden recibido. */
    public static List<String> warehousesWithStock(List<StockLine> stock) {
        Set<String> set = new LinkedHashSet<>();
        if (stock != null) {
            for (StockLine line : stock) {
                if (line != null && Math.abs(line.quantity()) > 1e-9 && !isBlank(line.warehouseId())) {
                    set.add(line.warehouseId().trim());
                }
            }
        }
        return List.copyOf(set);
    }

    /**
     * Existencia separada en parte de uso y parte rechazada. Las cantidades son null cuando son cero;
     * {@code rejectedPlaces} lista "REM" o "MPM/Rechazo" sin repetir.
     */
    public record StockSplit(Double usableQuantity, Double rejectedQuantity, List<String> rejectedPlaces) {}

    public static StockSplit split(List<StockLine> stock) {
        double usable = 0d;
        double rejected = 0d;
        Set<String> places = new LinkedHashSet<>();
        if (stock != null) {
            for (StockLine line : stock) {
                if (line == null || Math.abs(line.quantity()) <= 1e-9 || isBlank(line.warehouseId())) {
                    continue;
                }
                String wh = normalizeWarehouse(line.warehouseId());
                if (REJECT_WAREHOUSES.contains(wh)) {
                    rejected += line.quantity();
                    places.add(line.warehouseId().trim());
                } else if (OPERABLE_WAREHOUSES.contains(wh)) {
                    if (isRejectLocation(line.locationId())) {
                        rejected += line.quantity();
                        places.add(line.warehouseId().trim() + "/" + line.locationId().trim());
                    } else {
                        usable += line.quantity();
                    }
                }
            }
        }
        return new StockSplit(
                Math.abs(usable) > 1e-9 ? usable : null,
                Math.abs(rejected) > 1e-9 ? rejected : null,
                List.copyOf(places));
    }

    private static String rejectRule(String warehouse) {
        String wh = normalizeWarehouse(warehouse);
        if ("REM".equals(wh)) {
            return RULE_WAREHOUSE_REM;
        }
        if ("RES".equals(wh)) {
            return RULE_WAREHOUSE_RES;
        }
        return RULE_WAREHOUSE_REJECT;
    }

    static String normalizeWarehouse(String warehouse) {
        if (isBlank(warehouse)) {
            return "";
        }
        return warehouse.trim().toUpperCase(Locale.ROOT);
    }

    static boolean isRejectLocation(String locationId) {
        return !isBlank(locationId) && locationId.trim().toUpperCase(Locale.ROOT).startsWith("RECHAZ");
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

    static boolean isFailedQualityStatus(String qualityOrderStatus) {
        if (isBlank(qualityOrderStatus)) {
            return false;
        }
        String s = qualityOrderStatus.trim().toUpperCase(Locale.ROOT);
        return "FAIL".equals(s) || "FAILED".equals(s);
    }

    static boolean isValidValidatedDateTime(String validatedDateTime) {
        if (isBlank(validatedDateTime)) {
            return false;
        }
        String trimmed = validatedDateTime.trim();
        return !trimmed.startsWith("1900-01-01");
    }

    static boolean isExpired(String batchExpirationDate, LocalDate today) {
        if (isBlank(batchExpirationDate) || today == null) {
            return false;
        }
        String trimmed = batchExpirationDate.trim();
        if (trimmed.length() < 10 || trimmed.startsWith("1900-01-01")) {
            return false;
        }
        try {
            return LocalDate.parse(trimmed.substring(0, 10)).isBefore(today);
        } catch (DateTimeParseException ex) {
            return false;
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
