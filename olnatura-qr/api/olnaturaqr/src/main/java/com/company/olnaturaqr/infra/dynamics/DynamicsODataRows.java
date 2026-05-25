package com.company.olnaturaqr.infra.dynamics;

public final class DynamicsODataRows {

    private DynamicsODataRows() {}

    public static class WarehousesOnHandV2Row {
        public String ItemNumber;
        public String ProductName;
        public String InventorySiteId;
        public String InventoryWarehouseId;
        public Double AvailableOnHandQuantity;
        public Double OnHandQuantity;
        public Double TotalAvailableQuantity;
    }

    public static class ReleasedProductMastersRow {
        public String ItemNumber;
        public String ProductType;
        public String ProductGroupId;
        public String ItemModelGroupId;
        public Integer ShelfLifePeriodDays;
        public String InventoryUnitSymbol;
    }

    public static class ItemBatchAttributeValuesV2Row {
        public String ItemNumber;
        public String ItemBatchNumber;
        public String ItemBatchAttributeId;
        public Double DecimalValue;
        public String DateValue;
        public String StringValue;
    }

    public static class ItemBatchesRow {
        public String ItemNumber;
        public String BatchNumber;
        public String BatchExpirationDate;
        public String BestBeforeDate;
        public String ManufacturingDate;
        public String MostRecentTestDate;
    }

    public static class QualityOrderLineResultsRow {
        public String QualityOrderNumber;
        public String QualityTestId;
        public Double ResultValue;
        public String TestResult;
        public Double ResultInventoryQuantity;
    }
}
