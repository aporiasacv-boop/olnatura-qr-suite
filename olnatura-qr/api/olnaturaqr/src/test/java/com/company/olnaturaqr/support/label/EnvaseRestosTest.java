package com.company.olnaturaqr.support.label;

import com.company.olnaturaqr.domain.qr.QrLabel;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvaseRestosTest {

    @Test
    void zeroRemaindersKeepsStandardFlow() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("100");
        EnvaseRestos.apply(q, List.of(), "100", 5);
        assertFalse(q.isRestosEnabled());
        assertNull(q.getCantidadResto());
        assertNull(q.getRestosCantidadesJson());
        assertEquals("100", EnvaseRestos.cantidadForEnvase(q, 1));
        assertEquals("100", EnvaseRestos.cantidadForEnvase(q, 5));
    }

    @Test
    void oneRemainderOnLastEnvase() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("100");
        EnvaseRestos.apply(q, List.of("5"), "100", 5);
        assertTrue(q.isRestosEnabled());
        assertEquals("5", q.getCantidadResto());
        assertEquals("100", EnvaseRestos.cantidadForEnvase(q, 4));
        assertEquals("5", EnvaseRestos.cantidadForEnvase(q, 5));
        assertTrue(EnvaseRestos.isCantidadMenorEnvase(q, 5));
        assertFalse(EnvaseRestos.isCantidadMenorEnvase(q, 4));
    }

    @Test
    void twoIndependentRemainders() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("100");
        EnvaseRestos.apply(q, List.of("5", "8"), "100", 5);
        assertEquals("100", EnvaseRestos.cantidadForEnvase(q, 3));
        assertEquals("5", EnvaseRestos.cantidadForEnvase(q, 4));
        assertEquals("8", EnvaseRestos.cantidadForEnvase(q, 5));
        assertEquals(List.of("5", "8"), EnvaseRestos.listOf(q));
    }

    @Test
    void threeIndependentRemainders() {
        QrLabel q = base(6);
        q.setCantidadPorEnvase("100");
        EnvaseRestos.apply(q, List.of("5", "8", "3"), "100", 6);
        assertEquals("100", EnvaseRestos.cantidadForEnvase(q, 3));
        assertEquals("5", EnvaseRestos.cantidadForEnvase(q, 4));
        assertEquals("8", EnvaseRestos.cantidadForEnvase(q, 5));
        assertEquals("3", EnvaseRestos.cantidadForEnvase(q, 6));
    }

    @Test
    void fourIndependentRemainders() {
        QrLabel q = base(6);
        q.setCantidadPorEnvase("100");
        EnvaseRestos.apply(q, List.of("5", "8", "3", "6"), "100", 6);
        assertEquals("100", EnvaseRestos.cantidadForEnvase(q, 2));
        assertEquals("5", EnvaseRestos.cantidadForEnvase(q, 3));
        assertEquals("8", EnvaseRestos.cantidadForEnvase(q, 4));
        assertEquals("3", EnvaseRestos.cantidadForEnvase(q, 5));
        assertEquals("6", EnvaseRestos.cantidadForEnvase(q, 6));
        assertEquals("6", q.getCantidadResto());
        for (int n = 1; n <= 6; n++) {
            assertFalse(EnvaseRestos.cantidadForEnvase(q, n).toUpperCase().contains("RESTOS"));
        }
    }

    @Test
    void fifthRemainderRejected() {
        QrLabel q = base(8);
        q.setCantidadPorEnvase("100");
        assertThrows(ResponseStatusException.class,
                () -> EnvaseRestos.apply(q, List.of("5", "8", "3", "6", "2"), "100", 8));
    }

    @Test
    void emptyQuantityRejected() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("100");
        assertThrows(ResponseStatusException.class,
                () -> EnvaseRestos.apply(q, List.of("5", "  "), "100", 5));
    }

    @Test
    void remainderMustBePositiveAndLessThanStandard() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("100");
        assertThrows(ResponseStatusException.class, () -> EnvaseRestos.apply(q, List.of("0"), "100", 5));
        assertThrows(ResponseStatusException.class, () -> EnvaseRestos.apply(q, List.of("100"), "100", 5));
        assertThrows(ResponseStatusException.class, () -> EnvaseRestos.apply(q, List.of("120"), "100", 5));
        assertThrows(ResponseStatusException.class, () -> EnvaseRestos.apply(q, List.of("5"), "100", 0));
    }

    @Test
    void fiveEnvasesWithOneRemainderMatchesExactDistribution() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("25");
        EnvaseRestos.apply(q, List.of("18"), "25", 5);
        assertEquals(List.of("25", "25", "25", "25", "18"), EnvaseRestos.quantities(q));
        assertEquals("118", EnvaseRestos.cantidadTotal(q));
        assertEquals("18", EnvaseRestos.cantidadForEnvase(q, 5));
        assertEquals("25", EnvaseRestos.cantidadForEnvase(q, 1));
    }

    @Test
    void extraRemainderSlotsDoNotCreateExtraEnvases() {
        QrLabel q = base(5);
        q.setCantidadPorEnvase("25");
        EnvaseRestos.apply(q, List.of("18"), "25", 5);
        assertEquals(5, EnvaseRestos.quantities(q).size());
    }

    private static QrLabel base(int total) {
        QrLabel q = new QrLabel();
        q.setEnvaseTotal(total);
        return q;
    }
}
