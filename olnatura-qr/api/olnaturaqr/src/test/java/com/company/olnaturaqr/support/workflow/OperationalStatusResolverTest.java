package com.company.olnaturaqr.support.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OperationalStatusResolverTest {

    @Test
    void remWinsEvenWithApprovedDisposition() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPM", "REM"), "MPM", "Aprobado", true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
        assertEquals("REM", r.warehouseApplied());
    }

    @Test
    void resRejected() {
        var r = OperationalStatusResolver.resolve(List.of("RES"), null, "Aprobado", true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén RES", r.ruleApplied());
    }

    @Test
    void cuarentenaWarehouse() {
        var r = OperationalStatusResolver.resolve(List.of("CUARENTENA"), null, null, true);
        assertEquals("CUARENTENA", r.status());
        assertEquals("Almacén CUARENTENA", r.ruleApplied());
    }

    @Test
    void lote3390EmptyDispositionOperationalWarehouse() {
        var r = OperationalStatusResolver.resolve(List.of("MPM"), "MPM", null, true);
        assertEquals("APROBADO", r.status());
        assertEquals("BatchDispositionCode", r.ruleApplied());
    }

    @Test
    void lote3447Aprobado() {
        var r = OperationalStatusResolver.resolve(List.of("MPM"), "MPM", "Aprobado", true);
        assertEquals("APROBADO", r.status());
        assertEquals("BatchDispositionCode", r.ruleApplied());
    }

    @Test
    void dispositionRechazadoWithoutRem() {
        var r = OperationalStatusResolver.resolve(List.of("MEM"), "MEM", "Rechazado", true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("BatchDispositionCode", r.ruleApplied());
    }

    @Test
    void noDynamics() {
        var r = OperationalStatusResolver.resolve(null, null, null, false);
        assertEquals("DESCONOCIDO", r.status());
        assertNull(r.warehouseApplied());
    }

    @Test
    void qualityWarehouseRem() {
        var r = OperationalStatusResolver.resolve(List.of(), "REM", "Aprobado", true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
    }
}
