package com.company.olnaturaqr.infra.dynamics;

/**
 * Cliente HTTP OData de Dynamics 365. Sin orquestación ni OAuth: solo llamadas
 * autenticadas con el Bearer que le pase el servicio de negocio.
 */
public interface DynamicsClient {

    record ItemBatchRecord(
            String itemNumber,
            String batchNumber,
            String batchExpirationDate
    ) {}

    record InventoryOnHandRecord(
            String itemNumber,
            String productName,
            Double availableOnHandQuantity,
            String inventorySiteId
    ) {}

    record QualityOrderRecord(
            String itemBatchNumber,
            String itemNumber,
            String qualityOrderStatus,
            String passedBatchDispositionCode,
            String warehouseId,
            String warehouseLocationId
    ) {}

    /** Unidad de inventario desde ReleasedProductsV2 (InventoryUnitSymbol). */
    record ReleasedProductRecord(
            String itemNumber,
            String inventoryUnitSymbol
    ) {}

    java.util.Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken);

    java.util.Optional<InventoryOnHandRecord> findInventorySitesOnHand(String itemNumber, String accessToken);

    java.util.Optional<QualityOrderRecord> findQualityOrderByItemBatch(String itemBatchNumber, String accessToken);

    java.util.Optional<ReleasedProductRecord> findReleasedProduct(String itemNumber, String accessToken);
}
