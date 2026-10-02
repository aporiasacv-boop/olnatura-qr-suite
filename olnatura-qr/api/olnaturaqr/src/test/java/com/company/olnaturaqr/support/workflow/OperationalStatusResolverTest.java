package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.support.workflow.OperationalStatusResolver.StockLine;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OperationalStatusResolverTest {

    private static final String VALIDATED = "2026-07-28T19:45:17Z";
    private static final String SENTINEL_1900 = "1900-01-01T00:00:00Z";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final String NOT_EXPIRED = "2028-01-01T12:00:00Z";
    private static final String EXPIRED = "2026-04-30T12:00:00Z";

    private static StockLine at(String warehouse, double qty) {
        return new StockLine(warehouse, "Disponible", qty);
    }

    private static OperationalStatusResolver.Result resolve(List<StockLine> stock, String qo, String validated) {
        return OperationalStatusResolver.resolve(stock, qo, validated, NOT_EXPIRED, TODAY, true);
    }

    // --- Existencia operable + rechazo (lote parcialmente rechazado, familia 3612) ---

    @Test
    void remWithOperableAndPassIsAprobadoWhenExperimentEnabled() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "Regla operable+REM/RES desactivada");
        var r = resolve(List.of(at("MPM", 100), at("REM", 5)), "Pass", VALIDATED);
        assertEquals("APROBADO", r.status());
        assertEquals("Almacén disponible + REM/RES", r.ruleApplied());
        assertEquals("REM", r.warehouseApplied());
    }

    @Test
    void resWithOperableAndPassIsAprobadoWhenExperimentEnabled() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "Regla operable+REM/RES desactivada");
        var r = resolve(List.of(at("MPS", 100), at("RES", 5)), "Pass", VALIDATED);
        assertEquals("APROBADO", r.status());
        assertEquals("Almacén disponible + REM/RES", r.ruleApplied());
        assertEquals("RES", r.warehouseApplied());
    }

    @Test
    void operablePlusRejectRuleFlagDefaultsToEnabledInThisBranch() {
        org.junit.jupiter.api.Assertions.assertTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "La regla operable+REM/RES debe estar ON");
    }

    @Test
    void remWithOperableButOpenIsRechazado() {
        var r = resolve(List.of(at("MEM", 10), at("REM", 5)), "Open", SENTINEL_1900);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
    }

    // --- E1 / E2: la existencia decide, no el historial ni el almacén de la orden de calidad ---

    @Test
    void passLotMovedEntirelyToRemIsRechazado() {
        // Caso 240923-MPM0003109: Pass en MPM, hoy 0 en MPM y 247,675 en REM. Antes salía APROBADO.
        var r = resolve(List.of(at("MPM", 0), new StockLine("REM", "General", 247675)), "Pass", VALIDATED);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
        assertEquals("REM", r.warehouseApplied());
    }

    @Test
    void remAloneIsRechazado() {
        var r = resolve(List.of(at("REM", 1)), "Pass", VALIDATED);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
    }

    @Test
    void resAloneIsRechazado() {
        var r = resolve(List.of(at("RES", 1)), "Pass", VALIDATED);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén RES", r.ruleApplied());
    }

    @Test
    void drivexpressRejectWarehouseIsRechazado() {
        var r = resolve(List.of(at("RES-D", 3)), "Pass", VALIDATED);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Existencia solo en almacén de rechazo", r.ruleApplied());
        assertEquals("RES-D", r.warehouseApplied());
    }

    @Test
    void rejectLocationInsideOperableWarehouseIsRechazado() {
        var r = resolve(List.of(new StockLine("MPM", "Rechazo", 40)), "Pass", VALIDATED);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Existencia solo en ubicación Rechazo", r.ruleApplied());
    }

    @Test
    void zeroQuantityInRemDoesNotReject() {
        var r = resolve(List.of(at("MPM", 50), at("REM", 0)), "Pass", VALIDATED);
        assertEquals("APROBADO", r.status());
        assertEquals("QualityOrder Pass + ValidatedDateTime", r.ruleApplied());
        assertEquals("MPM", r.warehouseApplied());
    }

    @Test
    void consumedLotKeepsQualityStatus() {
        var r = resolve(List.of(at("MPM", 0)), "Pass", VALIDATED);
        assertEquals("APROBADO", r.status());
    }

    // --- E3: Fail ---

    @Test
    void failInOperableWarehouseIsRechazado() {
        var r = resolve(List.of(at("MEM", 5)), "Fail", VALIDATED);
        assertEquals("RECHAZADO", r.status());
        assertEquals("QualityOrderStatus Fail", r.ruleApplied());
        assertEquals("MEM", r.warehouseApplied());
    }

    @Test
    void failWithoutStockIsRechazado() {
        var r = resolve(List.of(), "Fail", VALIDATED);
        assertEquals("RECHAZADO", r.status());
    }

    // --- Caducidad ---

    @Test
    void expiredPassLotIsRechazado() {
        org.junit.jupiter.api.Assumptions.assumeTrue(OperationalStatusResolver.ENABLE_EXPIRED_AS_REJECTED);
        var r = OperationalStatusResolver.resolve(
                List.of(at("MPM", 10)), "Pass", VALIDATED, EXPIRED, TODAY, true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Lote caducado", r.ruleApplied());
    }

    @Test
    void expiresTodayIsNotExpired() {
        var r = OperationalStatusResolver.resolve(
                List.of(at("MPM", 10)), "Pass", VALIDATED, "2026-10-02T12:00:00Z", TODAY, true);
        assertEquals("APROBADO", r.status());
    }

    @Test
    void sentinelExpirationIsIgnored() {
        var r = OperationalStatusResolver.resolve(
                List.of(at("MPM", 10)), "Pass", VALIDATED, "1900-01-01T12:00:00Z", TODAY, true);
        assertEquals("APROBADO", r.status());
    }

    // --- Cuarentena / aprobado ---

    @Test
    void openQualityOrderIsCuarentenaEvenInMps() {
        var r = resolve(List.of(at("MPS", 1)), "Open", SENTINEL_1900);
        assertEquals("CUARENTENA", r.status());
        assertEquals("QualityOrderStatus Open", r.ruleApplied());
        assertEquals("MPS", r.warehouseApplied());
    }

    @Test
    void openBeatsPassSignals() {
        var r = resolve(List.of(at("MPS", 1)), "Open", VALIDATED);
        assertEquals("CUARENTENA", r.status());
        assertEquals("QualityOrderStatus Open", r.ruleApplied());
    }

    @Test
    void passPlusValidatedIsAprobadoInEveryOperableWarehouse() {
        for (String wh : List.of("MEM", "MES", "MPM", "MPS")) {
            var r = resolve(List.of(at(wh, 1)), "Pass", VALIDATED);
            assertEquals("APROBADO", r.status(), wh);
            assertEquals("QualityOrder Pass + ValidatedDateTime", r.ruleApplied(), wh);
            assertEquals(wh, r.warehouseApplied(), wh);
        }
    }

    @Test
    void passWith1900ValidatedIsDesconocido() {
        var r = resolve(List.of(at("MPS", 1)), "Pass", SENTINEL_1900);
        assertEquals("DESCONOCIDO", r.status());
        assertEquals("Información insuficiente", r.ruleApplied());
    }

    @Test
    void passWithoutValidatedIsDesconocido() {
        var r = resolve(List.of(at("MEM", 1)), "Pass", null);
        assertEquals("DESCONOCIDO", r.status());
    }

    @Test
    void operableWithoutQualityIsCuarentena() {
        var r = resolve(List.of(at("MEM", 1)), null, null);
        assertEquals("CUARENTENA", r.status());
        assertEquals("Almacén operable sin QualityOrder", r.ruleApplied());
        assertEquals("MEM", r.warehouseApplied());
    }

    @Test
    void cuarentenaWarehouseAloneIsDesconocidoWithoutOpenOrPass() {
        var r = resolve(List.of(at("CUARENTENA", 1)), null, null);
        assertEquals("DESCONOCIDO", r.status());
        assertEquals("Información insuficiente", r.ruleApplied());
    }

    @Test
    void noStockAndNoQualityIsDesconocido() {
        var r = resolve(List.of(), null, null);
        assertEquals("DESCONOCIDO", r.status());
        assertNull(r.warehouseApplied());
    }

    @Test
    void noDynamics() {
        var r = OperationalStatusResolver.resolve(null, null, null, null, TODAY, false);
        assertEquals("DESCONOCIDO", r.status());
        assertNull(r.warehouseApplied());
    }

    @Test
    void warehousesWithStockSkipsZeroAndDuplicates() {
        var list = OperationalStatusResolver.warehousesWithStock(List.of(
                at("MPM", 0), at("REM", 5), new StockLine("REM", "General", 2), at("MPS", 1)));
        assertEquals(List.of("REM", "MPS"), list);
    }
}
