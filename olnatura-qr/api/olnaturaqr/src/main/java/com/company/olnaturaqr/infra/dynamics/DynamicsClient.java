package com.company.olnaturaqr.infra.dynamics;


public interface DynamicsClient {

    record ItemBatchRecord(
            String itemNumber,
            String batchNumber,
            String batchExpirationDate,
            
            String batchDispositionCode
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
            String warehouseLocationId,
            
            String validatedDateTime,
            
            String validatingPersonnelNumber
    ) {}

    
    record ReleasedProductRecord(
            String itemNumber,
            String inventoryUnitSymbol
    ) {}

    
    record BatchEntryDateRecord(
            String datePhysical
    ) {}

    
    record InventDimRecord(
            String inventDimId,
            String inventLocationId,
            String wmsLocationId
    ) {}

    java.util.Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken);

    java.util.Optional<InventoryOnHandRecord> findInventorySitesOnHand(String itemNumber, String accessToken);

    java.util.Optional<QualityOrderRecord> findQualityOrderByItemBatch(String itemBatchNumber, String accessToken);

    java.util.Optional<ReleasedProductRecord> findReleasedProduct(String itemNumber, String accessToken);

    
    java.util.Optional<BatchEntryDateRecord> findBatchEntryDate(String batchNumber, String accessToken);

    
    java.util.List<InventDimRecord> findInventDimsByBatch(String batchNumber, String accessToken);
}
