package com.company.olnaturaqr.infra.dynamics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicsLookupServiceTest {

    private static final String VALIDATED = "2026-07-28T19:45:17Z";

    /** Cliente falso: datos por artículo + lote, y registro de las consultas de orden de calidad. */
    private static final class FakeClient implements DynamicsClient {
        final List<ItemBatchRecord> batches = new ArrayList<>();
        final Map<String, List<BatchOnHandRecord>> onHand = new java.util.HashMap<>();
        final Map<String, QualityOrderRecord> latestQuality = new java.util.HashMap<>();
        final List<String> qualityLookups = new ArrayList<>();

        static String key(String item, String batch) {
            return item + "|" + batch;
        }

        @Override
        public Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken) {
            return findItemBatches(batchNumber, accessToken).stream().findFirst();
        }

        @Override
        public List<ItemBatchRecord> findItemBatches(String batchNumber, String accessToken) {
            return batches.stream().filter(b -> b.batchNumber().equals(batchNumber)).toList();
        }

        @Override
        public Optional<InventoryOnHandRecord> findInventorySitesOnHand(String itemNumber, String accessToken) {
            // Existencia del artículo completo (todos sus lotes): no debe usarse como cantidad del lote.
            return Optional.of(new InventoryOnHandRecord(itemNumber, "Producto " + itemNumber, 999999d, "OLNATURA"));
        }

        @Override
        public List<BatchOnHandRecord> findBatchOnHand(String itemNumber, String batchNumber, String accessToken) {
            return onHand.getOrDefault(key(itemNumber, batchNumber), List.of());
        }

        @Override
        public Optional<QualityOrderRecord> findQualityOrderByItemBatch(String itemBatchNumber, String accessToken) {
            throw new AssertionError("El lookup no debe buscar la orden de calidad solo por número de lote");
        }

        @Override
        public Optional<QualityOrderRecord> findLatestQualityOrder(String itemNumber, String itemBatchNumber, String accessToken) {
            qualityLookups.add(key(itemNumber, itemBatchNumber));
            return Optional.ofNullable(latestQuality.get(key(itemNumber, itemBatchNumber)));
        }

        @Override
        public Optional<ReleasedProductRecord> findReleasedProduct(String itemNumber, String accessToken) {
            return Optional.of(new ReleasedProductRecord(itemNumber, "PZA"));
        }

        @Override
        public Optional<BatchEntryDateRecord> findBatchEntryDate(String batchNumber, String accessToken) {
            return Optional.empty();
        }

        @Override
        public List<InventDimRecord> findInventDimsByBatch(String batchNumber, String accessToken) {
            return List.of();
        }
    }

    private static DynamicsLookupService service(FakeClient client) {
        DynamicsProperties props = new DynamicsProperties();
        props.setMode("mock");
        DynamicsLookupService s = new DynamicsLookupService(
                client, props, new StaticListableBeanFactory().getBeanProvider(DynamicsOAuthTokenClient.class));
        s.setClock(Clock.fixed(Instant.parse("2026-10-02T18:00:00Z"), ZoneOffset.UTC));
        return s;
    }

    private static DynamicsClient.QualityOrderRecord pass(String item, String batch, String warehouse) {
        return new DynamicsClient.QualityOrderRecord(batch, item, "Pass", "Aprobado", warehouse, "Disponible", VALIDATED, "3960");
    }

    @Test
    void lotMovedToRemAfterPassIsRechazadoAndShowsLotQuantity() {
        FakeClient c = new FakeClient();
        c.batches.add(new DynamicsClient.ItemBatchRecord("106623700100", "240923-MPM0003109", "2027-04-30T12:00:00Z", null));
        c.onHand.put(FakeClient.key("106623700100", "240923-MPM0003109"), List.of(
                new DynamicsClient.BatchOnHandRecord("106623700100", "MPM", "Disponible", 0d),
                new DynamicsClient.BatchOnHandRecord("106623700100", "REM", "General", 247675d)));
        c.latestQuality.put(FakeClient.key("106623700100", "240923-MPM0003109"), pass("106623700100", "240923-MPM0003109", "MPM"));

        DynamicsLookupDto dto = service(c).lookupByBatchNumber("240923-MPM0003109").orElseThrow();

        assertEquals("RECHAZADO", dto.operationalStatus());
        assertEquals("Almacén REM", dto.operationalStatusRule());
        assertEquals(247675d, dto.cantidadAlmacen());
        assertEquals(List.of("REM"), dto.warehouses());
        assertEquals("REM", dto.almacen());
        assertNull(dto.fechaLiberacion());
    }

    @Test
    void mixedLotIsAprobadoAndReportsBothParts() {
        FakeClient c = new FakeClient();
        c.batches.add(new DynamicsClient.ItemBatchRecord("100623401100", "260206-MPM0003363", "2027-04-03T12:00:00Z", "Aprobado"));
        c.onHand.put(FakeClient.key("100623401100", "260206-MPM0003363"), List.of(
                new DynamicsClient.BatchOnHandRecord("100623401100", "MPM", "Disponible", 1000d),
                new DynamicsClient.BatchOnHandRecord("100623401100", "REM", "General", 25d)));
        c.latestQuality.put(FakeClient.key("100623401100", "260206-MPM0003363"), pass("100623401100", "260206-MPM0003363", "MPM"));

        DynamicsLookupDto dto = service(c).lookupByBatchNumber("260206-MPM0003363").orElseThrow();

        assertEquals("APROBADO", dto.operationalStatus());
        assertEquals("Almacén disponible + REM/RES", dto.operationalStatusRule());
        assertEquals(1025d, dto.cantidadAlmacen());
        assertEquals(1000d, dto.cantidadAprobada());
        assertEquals(25d, dto.cantidadRechazada());
        assertEquals(List.of("REM"), dto.almacenesRechazo());
    }

    @Test
    void itemHintPicksTheLabelItemWhenBatchNumberIsShared() {
        FakeClient c = new FakeClient();
        c.batches.add(new DynamicsClient.ItemBatchRecord("501235000100", "2407025", null, null));
        c.batches.add(new DynamicsClient.ItemBatchRecord("901235000100", "2407025", null, null));
        c.onHand.put(FakeClient.key("901235000100", "2407025"), List.of(
                new DynamicsClient.BatchOnHandRecord("901235000100", "MPS", "Disponible", 10d)));
        c.latestQuality.put(FakeClient.key("901235000100", "2407025"), pass("901235000100", "2407025", "MPS"));

        DynamicsLookupDto dto = service(c).lookupByBatchNumber("2407025", "901235000100").orElseThrow();

        assertEquals("901235000100", dto.codigo());
        assertEquals("APROBADO", dto.operationalStatus());
        assertEquals(List.of("901235000100|2407025"), c.qualityLookups);
    }

    @Test
    void withoutHintSharedBatchPicksTheItemThatHasStock() {
        FakeClient c = new FakeClient();
        c.batches.add(new DynamicsClient.ItemBatchRecord("301000000100", "251204-MES0001", null, null));
        c.batches.add(new DynamicsClient.ItemBatchRecord("401000000100", "251204-MES0001", null, null));
        c.onHand.put(FakeClient.key("401000000100", "251204-MES0001"), List.of(
                new DynamicsClient.BatchOnHandRecord("401000000100", "MES", "Disponible", 3d)));

        DynamicsLookupDto dto = service(c).lookupByBatchNumber("251204-MES0001").orElseThrow();

        assertEquals("401000000100", dto.codigo());
        assertEquals("CUARENTENA", dto.operationalStatus());
        assertEquals(3d, dto.cantidadAlmacen());
    }

    @Test
    void failIsRechazadoAndExpiredPassIsRechazado() {
        FakeClient c = new FakeClient();
        c.batches.add(new DynamicsClient.ItemBatchRecord("400615440900", "260619-MEM0003625", "2027-06-19T12:00:00Z", null));
        c.batches.add(new DynamicsClient.ItemBatchRecord("100623401100", "260206-MPM0003363", "2026-04-03T12:00:00Z", "Aprobado"));
        c.onHand.put(FakeClient.key("400615440900", "260619-MEM0003625"), List.of(
                new DynamicsClient.BatchOnHandRecord("400615440900", "MEM", "Disponible", 5d)));
        c.onHand.put(FakeClient.key("100623401100", "260206-MPM0003363"), List.of(
                new DynamicsClient.BatchOnHandRecord("100623401100", "MPM", "Disponible", 20d)));
        c.latestQuality.put(FakeClient.key("400615440900", "260619-MEM0003625"),
                new DynamicsClient.QualityOrderRecord("260619-MEM0003625", "400615440900", "Fail", "Rechazado", "MEM", "Disponible", VALIDATED, "3960"));
        c.latestQuality.put(FakeClient.key("100623401100", "260206-MPM0003363"), pass("100623401100", "260206-MPM0003363", "MPM"));

        DynamicsLookupService s = service(c);
        assertEquals("RECHAZADO", s.lookupByBatchNumber("260619-MEM0003625").orElseThrow().operationalStatus());
        DynamicsLookupDto expired = s.lookupByBatchNumber("260206-MPM0003363").orElseThrow();
        assertEquals("RECHAZADO", expired.operationalStatus());
        assertEquals("Lote caducado", expired.operationalStatusRule());
    }

    @Test
    void lotWithoutStockReportsNullQuantityNotTheItemTotal() {
        FakeClient c = new FakeClient();
        c.batches.add(new DynamicsClient.ItemBatchRecord("106623850300", "260406-MPM0003390", "2027-04-01T12:00:00Z", null));
        c.latestQuality.put(FakeClient.key("106623850300", "260406-MPM0003390"), pass("106623850300", "260406-MPM0003390", "MPM"));

        DynamicsLookupDto dto = service(c).lookupByBatchNumber("260406-MPM0003390").orElseThrow();

        assertNull(dto.cantidadAlmacen());
        assertEquals("APROBADO", dto.operationalStatus());
        assertTrue(dto.warehouses().isEmpty());
    }
}
