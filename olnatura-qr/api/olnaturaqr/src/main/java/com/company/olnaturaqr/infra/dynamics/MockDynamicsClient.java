package com.company.olnaturaqr.infra.dynamics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
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
            String locationId,
            /** Almacenes InventDim (InventLocationId), p.ej. MPM + REM. */
            List<String> inventLocationIds
    ) {}

    private final Map<String, FakeBatch> fake = Map.ofEntries(
            Map.entry("260112-MES003456", new FakeBatch(
                    "MOCK-001", "Material demo A", "2027-01-01T12:00:00Z", "2026-01-12T12:00:00Z", "CUARENTENA",
                    50.0, "SITE-A", "Open", "", "ALM-A", "Almacén principal", List.of("ALM-A"))),
            Map.entry("LOTE-TEST-001", new FakeBatch(
                    "MOCK-002", "Material demo B", "2027-06-01T12:00:00Z", "2026-02-01T12:00:00Z", "Disponible",
                    100.0, "SITE-B", "Pass", "Disponible", "ALM-B", "Almacén A", List.of("ALM-B"))),
            Map.entry("251201-MEM0003454", new FakeBatch(
                    "MOCK-003", "Material demo C", "2026-12-01T12:00:00Z", "2025-12-01T12:00:00Z", "Disponible",
                    20.0, "SITE-C", "Pass", "Disponible", "ALM-C", "Almacén secundario", List.of("ALM-C"))),
            Map.entry("260713-MEM0003662", new FakeBatch(
                    "400615440900", "QG5 FOLLETO", "2027-01-02T12:00:00Z", "2026-07-13T12:00:00Z", "",
                    62738.0, "OLNATURA", "Open", "", "MEM", "Disponible", List.of("MEM"))),
            Map.entry("260721-MPS0006642", new FakeBatch(
                    "MOCK-MPS-6642", "Material prueba MPS", "2027-07-21T12:00:00Z", "2026-07-21T12:00:00Z", "",
                    10.0, "OLNATURA", "Open", "", "MPS", "Disponible", List.of("MPS"))),
            Map.entry("260724-MEM0003673", new FakeBatch(
                    "MOCK-MEM-3673", "Diag cuarentena", "2027-07-24T12:00:00Z", "2026-07-24T12:00:00Z", "Cuarentena",
                    5.0, "OLNATURA", "Open", "", "MEM", "Cuarentena", List.of("MEM"))),
            Map.entry("260713-MEM0003664", new FakeBatch(
                    "MOCK-MEM-3664", "Diag aprobado", "2027-07-13T12:00:00Z", "2026-07-13T12:00:00Z", "Disponible",
                    5.0, "OLNATURA", "Pass", "Disponible", "MEM", "Disponible", List.of("MEM"))),
            Map.entry("260619-MEM0003625", new FakeBatch(
                    "MOCK-MEM-3625", "Diag rechazado", "2027-06-19T12:00:00Z", "2026-06-19T12:00:00Z", "Rechazado",
                    5.0, "OLNATURA", "Fail", "Rechazado", "MEM", "Rechazado", List.of("MEM"))),
            // Lotes de validación Estado Operativo (espejo de evidencia live)
            Map.entry("260406-MPM0003390", new FakeBatch(
                    "106623850300", "CROSPOVIDONA XL-10 (TIPO B)", "2027-04-01T12:00:00Z", "2026-04-06T12:00:00Z", "",
                    49082.0, "OLNATURA", "Pass", "", "MPM", "Disponible", List.of("MPM"))),
            Map.entry("260707-MPM0003447", new FakeBatch(
                    "100623400601", "SORBITOL", "2029-02-12T12:00:00Z", "2026-07-07T12:00:00Z", "Aprobado",
                    6999157.0, "OLNATURA", "Pass", "Aprobado", "MPM", "Disponible", List.of("MPM"))),
            Map.entry("260206-MPM0003363", new FakeBatch(
                    "100623401100", "SABOR VAINILLA ARO", "2026-04-03T12:00:00Z", "2026-02-06T12:00:00Z", "Aprobado",
                    138806.0, "OLNATURA", "Pass", "Aprobado", "MPM", "Disponible", List.of("MPM", "REM"))),
            Map.entry("240923-MPM0003109", new FakeBatch(
                    "106623700100", "TAMSULOSINA HCL PELLETS 0.2%", "2026-04-30T12:00:00Z", "2024-09-23T12:00:00Z", "",
                    267100.0, "OLNATURA", "Pass", "", "MPM", "Disponible", List.of("MPM", "REM")))
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

    @Override
    public List<InventDimRecord> findInventDimsByBatch(String batchNumber, String accessToken) {
        FakeBatch b = fake.get(batchNumber);
        if (b == null || b.inventLocationIds() == null || b.inventLocationIds().isEmpty()) {
            return List.of();
        }
        return b.inventLocationIds().stream()
                .map(loc -> new InventDimRecord(
                        "MOCK-DIM-" + batchNumber + "-" + loc,
                        loc,
                        "REM".equalsIgnoreCase(loc) || "RES".equalsIgnoreCase(loc) ? "General" : "Disponible"
                ))
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
