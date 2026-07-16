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
            double qty,
            String siteId,
            String qualityStatus,
            String warehouseId,
            String locationId
    ) {}

    private final Map<String, FakeBatch> fake = Map.ofEntries(
            Map.entry("260112-MES003456", new FakeBatch(
                    "MOCK-001", "Material demo A", "2027-01-01T12:00:00Z",
                    50.0, "SITE-A", "CUARENTENA", "ALM-A", "Almacén principal")),
            Map.entry("LOTE-TEST-001", new FakeBatch(
                    "MOCK-002", "Material demo B", "2027-06-01T12:00:00Z",
                    100.0, "SITE-B", "LIBERADO", "ALM-B", "Almacén A")),
            Map.entry("251201-MEM0003454", new FakeBatch(
                    "MOCK-003", "Material demo C", "2026-12-01T12:00:00Z",
                    20.0, "SITE-C", "LIBERADO", "ALM-C", "Almacén secundario")),
            Map.entry("260713-MEM0003662", new FakeBatch(
                    "400615440900", "QG5 FOLLETO", "2027-01-02T12:00:00Z",
                    62738.0, "OLNATURA", "Open", "MEM", "Disponible"))
    );

    @Override
    public Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken) {
        FakeBatch b = fake.get(batchNumber);
        if (b == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemBatchRecord(b.itemNumber(), batchNumber, b.expiration()));
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
                "",
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
}
