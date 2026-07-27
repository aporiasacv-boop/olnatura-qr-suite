package com.company.olnaturaqr.support.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminStatusCorrectionServiceTest {

    @Test
    @DisplayName("Transiciones permitidas")
    void allowedTransitions() {
        assertTrue(AdminStatusCorrectionService.isAllowed("CUARENTENA", "APROBADO"));
        assertTrue(AdminStatusCorrectionService.isAllowed("APROBADO", "CUARENTENA"));
        assertTrue(AdminStatusCorrectionService.isAllowed("RECHAZADO", "CUARENTENA"));
    }

    @Test
    @DisplayName("RECHAZADO → APROBADO nunca")
    void rejectedToApprovedForbidden() {
        assertFalse(AdminStatusCorrectionService.isAllowed("RECHAZADO", "APROBADO"));
    }

    @Test
    @DisplayName("Otras transiciones no permitidas")
    void otherForbidden() {
        assertFalse(AdminStatusCorrectionService.isAllowed("CUARENTENA", "RECHAZADO"));
        assertFalse(AdminStatusCorrectionService.isAllowed("APROBADO", "RECHAZADO"));
        assertFalse(AdminStatusCorrectionService.isAllowed("RECHAZADO", "RECHAZADO"));
        assertFalse(AdminStatusCorrectionService.isAllowed("CUARENTENA", "CUARENTENA"));
    }

    @Test
    @DisplayName("Destinos desde cada estado")
    void targets() {
        assertEquals(List.of(WorkflowStatus.APROBADO), AdminStatusCorrectionService.allowedTargets("CUARENTENA"));
        assertEquals(List.of(WorkflowStatus.CUARENTENA), AdminStatusCorrectionService.allowedTargets("APROBADO"));
        assertEquals(List.of(WorkflowStatus.CUARENTENA), AdminStatusCorrectionService.allowedTargets("RECHAZADO"));
    }
}
