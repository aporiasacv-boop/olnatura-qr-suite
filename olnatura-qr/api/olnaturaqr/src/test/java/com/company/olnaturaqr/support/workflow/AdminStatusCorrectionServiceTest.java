package com.company.olnaturaqr.support.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminStatusCorrectionServiceTest {

    @Test
    @DisplayName("Ninguna transición manual de estado operativo")
    void noManualTransitions() {
        assertFalse(AdminStatusCorrectionService.isAllowed("CUARENTENA", "APROBADO"));
        assertFalse(AdminStatusCorrectionService.isAllowed("APROBADO", "CUARENTENA"));
        assertFalse(AdminStatusCorrectionService.isAllowed("RECHAZADO", "CUARENTENA"));
        assertFalse(AdminStatusCorrectionService.isAllowed("RECHAZADO", "APROBADO"));
        assertEquals(List.of(), AdminStatusCorrectionService.allowedTargets("CUARENTENA"));
        assertEquals(List.of(), AdminStatusCorrectionService.allowedTargets("APROBADO"));
        assertEquals(List.of(), AdminStatusCorrectionService.allowedTargets("RECHAZADO"));
    }
}
