package com.company.olnaturaqr.api;

public record DynamicsPreviewResponse(
        String itemNumber,
        String productName,
        String productType,
        String productGroup,
        String warehouse,
        String site,
        Double availableQuantity,
        Double onHandQuantity,
        String unit,
        String batchNumber,
        String batchAttribute,
        String batchValue,
        String qualityResult,
        Double qualityValue
) {}
