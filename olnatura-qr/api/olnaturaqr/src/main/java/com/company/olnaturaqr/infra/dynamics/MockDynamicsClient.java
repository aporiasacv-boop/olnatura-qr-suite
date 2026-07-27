package com.company.olnaturaqr.infra.dynamics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockDynamicsClient implements DynamicsClient {

    private record FakeBatch(
            String itemNumber,
            String productName,
            String expiration,
            String fechaEntrada,
            String batchDispositionCode,
            double qty,
            String siteId,
            String qualityStatus,
            String passedBatchDispositionCode,
            String warehouseId,
            String locationId
    ) {}

    private final Map<String, FakeBatch> fake = Map.ofEntries(
            Map.entry("260112-MES003456", new FakeBatch(
                    "MOCK-001", "Material demo A", "2027-01-01T12:00:00Z", "2026-01-12T12:00:00Z", "CUARENTENA",
                    50.0, "SITE-A", "Open", "", "ALM-A", "Almacén principal")),
            Map.entry("LOTE-TEST-001", new FakeBatch(
                    "MOCK-002", "Material demo B", "2027-06-01T12:00:00Z", "2026-02-01T12:00:00Z", "Disponible",
                    100.0, "SITE-B", "Pass", "Disponible", "ALM-B", "Almacén A")),
            Map.entry("251201-MEM0003454", new FakeBatch(
                    "MOCK-003", "Material demo C", "2026-12-01T12:00:00Z", "2025-12-01T12:00:00Z", "Disponible",
                    20.0, "SITE-C", "Pass", "Disponible", "ALM-C", "Almacén secundario")),
            Map.entry("260713-MEM0003662", new FakeBatch(
                    "400615440900", "QG5 FOLLETO", "2027-01-02T12:00:00Z", "2026-07-13T12:00:00Z", "",
                    62738.0, "OLNATURA", "Open", "", "MEM", "Disponible")),
            Map.entry("260721-MPS0006642", new FakeBatch(
                    "MOCK-MPS-6642", "Material prueba MPS", "2027-07-21T12:00:00Z", "2026-07-21T12:00:00Z", "",
                    10.0, "OLNATURA", "Open", "", "MPS", "Disponible")),
            // Lotes de diagnóstico estado QR vs Dynamics
            Map.entry("260724-MEM0003673", new FakeBatch(
                    "MOCK-MEM-3673", "Diag cuarentena", "2027-07-24T12:00:00Z", "2026-07-24T12:00:00Z", "Cuarentena",
                    5.0, "OLNATURA", "Open", "", "MEM", "Cuarentena")),
            Map.entry("260713-MEM0003664", new FakeBatch(
                    "MOCK-MEM-3664", "Diag aprobado", "2027-07-13T12:00:00Z", "2026-07-13T12:00:00Z", "Disponible",
                    5.0, "OLNATURA", "Pass", "Disponible", "MEM", "Disponible")),
            Map.entry("260619-MEM0003625", new FakeBatch(
                    "MOCK-MEM-3625", "Diag rechazado", "2027-06-19T12:00:00Z", "2026-06-19T12:00:00Z", "Rechazado",
                    5.0, "OLNATURA", "Fail", "Rechazado", "MEM", "Rechazado"))
    );

    @Override
    public Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken) {
        FakeBatch b = fake.get(batchNumber);
        if (b == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemBatchRecord(
                b.itemNumber(), batchNumber, b.expiration(), blankToNull(b.batchDispositionCode())));
    }

    @Override
    public Optional<InventoryOnHandRecord> findInventorySitesOnHand(String itemNumber, String accessToken) {
        return fake.values().stream()
                .filter(b -> b.itemNumber().equals(itemNumber))
                .findFirst()
                .map(b -> new InventoryOnHandRecord(
                        b.itemNumber(), b.productName(), b.qty(), b.siteId()));
    }

    @Override
    public Optional<QualityOrderRecord> findQualityOrderByItemBatch(String itemBatchNumber, String accessToken) {
        FakeBatch b = fake.get(itemBatchNumber);
        if (b == null) {
            return Optional.empty();
        }
        return Optional.of(new QualityOrderRecord(
                itemBatchNumber,
                b.itemNumber(),
                b.qualityStatus(),
                b.passedBatchDispositionCode() != null ? b.passedBatchDispositionCode() : "",
                b.warehouseId(),
                b.locationId()
        ));
    }

    @Override
    public Optional<ReleasedProductRecord> findReleasedProduct(String itemNumber, String accessToken) {
        return fake.values().stream()
                .filter(b -> b.itemNumber().equals(itemNumber))
                .findFirst()
                .map(b -> new ReleasedProductRecord(b.itemNumber(), "PZA"));
    }

    @Override
    public Optional<BatchEntryDateRecord> findBatchEntryDate(String batchNumber, String accessToken) {
        FakeBatch b = fake.get(batchNumber);
        if (b == null || b.fechaEntrada() == null || b.fechaEntrada().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new BatchEntryDateRecord(b.fechaEntrada()));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
