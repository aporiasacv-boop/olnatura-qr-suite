package com.company.olnaturaqr.system;

import com.company.olnaturaqr.core.integrity.SignatureDigest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalIntegrityPresenceTest {

    @Test
    void shouldExposeStableSha256ExpectedDigest() {
        String expected = SignatureDigest.expectedDigest();
        assertFalse(expected.isBlank());
        assertTrue(expected.matches("^[a-f0-9]{64}$"));
    }
}
