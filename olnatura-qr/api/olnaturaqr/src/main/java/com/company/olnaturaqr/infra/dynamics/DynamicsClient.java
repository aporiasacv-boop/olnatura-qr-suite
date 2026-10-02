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
            String datePhysical,
            Double receivedQuantity
    ) {}

    
    record InventDimRecord(
            String inventDimId,
            String inventLocationId,
            String wmsLocationId
    ) {}

    /** Existencia de un artículo + lote en un almacén y ubicación (ProjInventoryOnHand, disponible físico). */
    record BatchOnHandRecord(
            String itemNumber,
            String warehouseId,
            String locationId,
            Double availablePhysicalQuantity
    ) {}

    java.util.Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken);

    /** Todos los artículos que usan ese número de lote: el número de lote no es único entre artículos. */
    java.util.List<ItemBatchRecord> findItemBatches(String batchNumber, String accessToken);

    java.util.Optional<InventoryOnHandRecord> findInventorySitesOnHand(String itemNumber, String accessToken);

    /** Existencia actual del artículo + lote por almacén y ubicación. */
    java.util.List<BatchOnHandRecord> findBatchOnHand(String itemNumber, String batchNumber, String accessToken);

    java.util.Optional<QualityOrderRecord> findQualityOrderByItemBatch(String itemBatchNumber, String accessToken);

    /** Orden de calidad más reciente (mayor QualityOrderNumber) del artículo + lote. */
    java.util.Optional<QualityOrderRecord> findLatestQualityOrder(String itemNumber, String itemBatchNumber, String accessToken);

    java.util.Optional<ReleasedProductRecord> findReleasedProduct(String itemNumber, String accessToken);

    
    java.util.Optional<BatchEntryDateRecord> findBatchEntryDate(String batchNumber, String accessToken);

    
    java.util.List<InventDimRecord> findInventDimsByBatch(String batchNumber, String accessToken);
}
