package com.company.olnaturaqr.support.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OperationalStatusSyncServiceTest {

    @Test
    void canonicalizeStoresDefinitiveStatusesAndMapsParcialToAprobado() {
        assertEquals("APROBADO", OperationalStatusSyncService.canonicalizeForStorage("aprobado"));
        assertEquals("CUARENTENA", OperationalStatusSyncService.canonicalizeForStorage("CUARENTENA"));
        assertEquals("RECHAZADO", OperationalStatusSyncService.canonicalizeForStorage("RECHAZADO"));
        assertEquals("APROBADO", OperationalStatusSyncService.canonicalizeForStorage("PARCIAL"));
    }

    @Test
    void canonicalizeSkipsDesconocidoAndBlank() {
        assertNull(OperationalStatusSyncService.canonicalizeForStorage(null));
        assertNull(OperationalStatusSyncService.canonicalizeForStorage(""));
        assertNull(OperationalStatusSyncService.canonicalizeForStorage("DESCONOCIDO"));
        assertNull(OperationalStatusSyncService.canonicalizeForStorage("OTRO"));
    }

    @Test
    void normalizeStoredMapsParcialToAprobado() {
        assertEquals("APROBADO", OperationalStatusSyncService.normalizeStored("parcial"));
        assertEquals("CUARENTENA", OperationalStatusSyncService.normalizeStored(null));
        assertEquals("CUARENTENA", OperationalStatusSyncService.normalizeStored("PENDING"));
    }
}
