package com.company.olnaturaqr.support.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalStatusPresentationTest {

    @Test
    void keepsCanonicalStatuses() {
        assertEquals("APROBADO", OperationalStatusPresentation.forUi("APROBADO"));
        assertEquals("RECHAZADO", OperationalStatusPresentation.forUi("rechazado"));
        assertEquals("CUARENTENA", OperationalStatusPresentation.forUi("CUARENTENA"));
        assertEquals("APROBADO", OperationalStatusPresentation.forUi("parcial"));
    }

    @Test
    void mapsDesconocidoAndBlankToCuarentena() {
        assertEquals("CUARENTENA", OperationalStatusPresentation.forUi("DESCONOCIDO"));
        assertEquals("CUARENTENA", OperationalStatusPresentation.forUi(null));
        assertEquals("CUARENTENA", OperationalStatusPresentation.forUi("  "));
        assertEquals("CUARENTENA", OperationalStatusPresentation.forUi("OTRO"));
    }
}
