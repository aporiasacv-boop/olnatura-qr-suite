package com.company.olnaturaqr.support.label;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelPrintDatesTest {

    @Test
    void formatsLabelDatesAsDdMmmYy() {
        assertEquals("10/SEP/26", LabelPrintDates.formatDdMmmYy(LocalDate.of(2026, 9, 10)));
        assertEquals("01/ENE/26", LabelPrintDates.formatDdMmmYy(LocalDate.of(2026, 1, 1)));
        assertEquals("N/A", LabelPrintDates.formatDdMmmYy(null));
    }
}
