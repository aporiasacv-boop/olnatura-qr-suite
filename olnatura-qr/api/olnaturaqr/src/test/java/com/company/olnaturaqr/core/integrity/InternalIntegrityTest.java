package com.company.olnaturaqr.core.integrity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalIntegrityTest {

    @Test
    void shouldMatchExpectedInternalDigest() {
        String expected = SignatureDigest.expectedDigest();
        String current = SignatureDigest.computeCurrentDigest();
        assertFalse(expected.isBlank());
        assertTrue(expected.matches("^[a-f0-9]{64}$"));
        assertEquals(expected, current);
    }
}
