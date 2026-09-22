package com.company.olnaturaqr.support.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OperationalStatusResolverTest {

    private static final String VALIDATED = "2026-07-28T19:45:17Z";
    private static final String SENTINEL_1900 = "1900-01-01T00:00:00Z";

    @Test
    void remWithOperableAndPassIsAprobadoWhenExperimentEnabled() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "Regla operable+REM/RES desactivada");
        var r = OperationalStatusResolver.resolve(
                List.of("MPM", "REM"), "MPM", "Aprobado", "Pass", VALIDATED, true);
        assertEquals("APROBADO", r.status());
        assertEquals("Almacén disponible + REM/RES", r.ruleApplied());
        assertEquals("REM", r.warehouseApplied());
    }

    @Test
    void resWithOperableAndPassIsAprobadoWhenExperimentEnabled() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "Regla operable+REM/RES desactivada");
        var r = OperationalStatusResolver.resolve(
                List.of("MPS", "RES"), "MPS", "Aprobado", "Pass", VALIDATED, true);
        assertEquals("APROBADO", r.status());
        assertEquals("Almacén disponible + REM/RES", r.ruleApplied());
        assertEquals("RES", r.warehouseApplied());
    }

    @Test
    void memRemPassLike3612FamilyIsAprobadoWhenExperimentEnabled() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "Regla operable+REM/RES desactivada");
        var r = OperationalStatusResolver.resolve(
                List.of("MEM", "REM"), "MEM", null, "Pass", VALIDATED, true);
        assertEquals("APROBADO", r.status());
        assertEquals("Almacén disponible + REM/RES", r.ruleApplied());
    }

    @Test
    void operablePlusRejectRuleFlagDefaultsToEnabledInThisBranch() {
        org.junit.jupiter.api.Assertions.assertTrue(
                OperationalStatusResolver.ENABLE_PARTIAL_STATE_EXPERIMENT,
                "La regla operable+REM/RES debe estar ON");
    }

    @Test
    void remAloneIsRechazado() {
        var r = OperationalStatusResolver.resolve(
                List.of("REM"), "REM", null, "Pass", VALIDATED, true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
    }

    @Test
    void resAloneIsRechazado() {
        var r = OperationalStatusResolver.resolve(
                List.of("RES"), "RES", null, "Pass", VALIDATED, true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén RES", r.ruleApplied());
    }

    @Test
    void remWithOperableButOpenIsRechazado() {
        var r = OperationalStatusResolver.resolve(
                List.of("MEM", "REM"), "MEM", null, "Open", SENTINEL_1900, true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
    }

    @Test
    void openQualityOrderIsCuarentenaEvenInMps() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPS"), "MPS", null, "Open", SENTINEL_1900, true);
        assertEquals("CUARENTENA", r.status());
        assertEquals("QualityOrderStatus Open", r.ruleApplied());
        assertEquals("MPS", r.warehouseApplied());
    }

    @Test
    void mps0006649Open() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPS"), "MPS", "", "Open", SENTINEL_1900, true);
        assertEquals("CUARENTENA", r.status());
    }

    @Test
    void openBeatsPassSignals() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPS"), "MPS", "Aprobado", "Open", VALIDATED, true);
        assertEquals("CUARENTENA", r.status());
        assertEquals("QualityOrderStatus Open", r.ruleApplied());
    }

    @Test
    void passPlusValidatedMemIsAprobado() {
        var r = OperationalStatusResolver.resolve(
                List.of("MEM"), "MEM", null, "Pass", VALIDATED, true);
        assertEquals("APROBADO", r.status());
        assertEquals("QualityOrder Pass + ValidatedDateTime", r.ruleApplied());
        assertEquals("MEM", r.warehouseApplied());
    }

    @Test
    void passPlusValidatedMesIsAprobado() {
        var r = OperationalStatusResolver.resolve(
                List.of("MES"), "MES", "", "Pass", "2026-07-28T19:44:01Z", true);
        assertEquals("APROBADO", r.status());
        assertEquals("QualityOrder Pass + ValidatedDateTime", r.ruleApplied());
    }

    @Test
    void passPlusValidatedMpmIsAprobado() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPM"), "MPM", null, "Pass", "2026-07-27T21:43:16Z", true);
        assertEquals("APROBADO", r.status());
        assertEquals("QualityOrder Pass + ValidatedDateTime", r.ruleApplied());
    }

    @Test
    void passPlusValidatedMpsIsAprobado() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPS"), "MPS", "Aprobado", "Pass", "2026-07-27T21:52:43Z", true);
        assertEquals("APROBADO", r.status());
        assertEquals("QualityOrder Pass + ValidatedDateTime", r.ruleApplied());
    }

    @Test
    void passWith1900ValidatedIsDesconocido() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPS"), "MPS", null, "Pass", SENTINEL_1900, true);
        assertEquals("DESCONOCIDO", r.status());
        assertEquals("Información insuficiente", r.ruleApplied());
    }

    @Test
    void passWithoutValidatedIsDesconocido() {
        var r = OperationalStatusResolver.resolve(
                List.of("MEM"), "MEM", null, "Pass", null, true);
        assertEquals("DESCONOCIDO", r.status());
        assertEquals("Información insuficiente", r.ruleApplied());
    }

    @Test
    void dispositionIgnoredWhenPassAndValidated() {
        var r = OperationalStatusResolver.resolve(
                List.of("MPM"), "MPM", null, "Pass", "2026-07-27T21:43:16Z", true);
        assertEquals("APROBADO", r.status());
    }

    @Test
    void warehouseOnlyWithoutQualityIsCuarentena() {
        var r = OperationalStatusResolver.resolve(List.of("MPS"), "MPS", null, null, null, true);
        assertEquals("CUARENTENA", r.status());
        assertEquals("Almacén operable sin QualityOrder", r.ruleApplied());
    }

    @Test
    void memWithoutQualityOrderIsCuarentena() {
        var r = OperationalStatusResolver.resolve(List.of("MEM"), "MEM", null, null, null, true);
        assertEquals("CUARENTENA", r.status());
        assertEquals("Almacén operable sin QualityOrder", r.ruleApplied());
        assertEquals("MEM", r.warehouseApplied());
    }

    @Test
    void cuarentenaWarehouseAloneIsDesconocidoWithoutOpenOrPass() {
        var r = OperationalStatusResolver.resolve(List.of("CUARENTENA"), null, null, null, null, true);
        assertEquals("DESCONOCIDO", r.status());
        assertEquals("Información insuficiente", r.ruleApplied());
    }

    @Test
    void noDynamics() {
        var r = OperationalStatusResolver.resolve(null, null, null, null, null, false);
        assertEquals("DESCONOCIDO", r.status());
        assertNull(r.warehouseApplied());
    }

    @Test
    void qualityWarehouseRem() {
        var r = OperationalStatusResolver.resolve(
                List.of(), "REM", "Aprobado", "Pass", VALIDATED, true);
        assertEquals("RECHAZADO", r.status());
        assertEquals("Almacén REM", r.ruleApplied());
    }

    @Test
    void legacyFourArgOverloadDelegatesWithoutQualityStatus() {
        var r = OperationalStatusResolver.resolve(List.of("MPS"), "MPS", null, true);
        assertEquals("CUARENTENA", r.status());
        assertEquals("Almacén operable sin QualityOrder", r.ruleApplied());
    }

    @Test
    void legacyFiveArgOverloadWithoutValidatedIsDesconocidoOnPass() {
        var r = OperationalStatusResolver.resolve(List.of("MEM"), "MEM", null, "Pass", true);
        assertEquals("DESCONOCIDO", r.status());
    }

    @Test
    void validatedLotsFromCalidadAreAprobado() {
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MEM"), "MEM", null, "Pass", "2026-07-28T19:45:17Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MES"), "MES", null, "Pass", "2026-07-28T19:44:01Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MEM"), "MEM", null, "Pass", "2026-07-28T19:44:40Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MPM"), "MPM", null, "Pass", "2026-07-27T21:43:16Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MPM"), "MPM", null, "Pass", "2026-07-27T21:45:58Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MPM"), "MPM", "Aprobado", "Pass", "2026-07-27T21:47:07Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MPM"), "MPM", "Aprobado", "Pass", "2026-07-28T14:04:20Z", true).status());
        assertEquals("APROBADO", OperationalStatusResolver.resolve(
                List.of("MPS"), "MPS", "Aprobado", "Pass", "2026-07-27T21:52:43Z", true).status());
    }
}
