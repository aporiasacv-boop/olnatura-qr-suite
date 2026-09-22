package com.company.olnaturaqr.support.label;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelDocumentCodeTest {

    @Test
    void upgradesLegacyPrefixAndKeepsRemainder() {
        assertEquals("AL-001-E02/06", LabelDocumentCode.resolve(null));
        assertEquals("AL-001-E02/06", LabelDocumentCode.resolve(""));
        assertEquals("AL-001-E02/06", LabelDocumentCode.resolve("AL-001-E02/04"));
        assertEquals("AL-001-E02/06 extra", LabelDocumentCode.resolve("AL-001-E02/04 extra"));
        assertTrue(LabelDocumentCode.resolve("AL-001-E02/06").startsWith("AL-001-E02/06"));
    }
}
