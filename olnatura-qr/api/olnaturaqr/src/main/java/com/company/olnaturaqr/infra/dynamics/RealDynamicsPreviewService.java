package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.api.DynamicsPreviewResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static com.company.olnaturaqr.infra.dynamics.DynamicsODataRows.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class RealDynamicsPreviewService implements DynamicsPreviewService {

    private final DynamicsODataClient odata;

    public RealDynamicsPreviewService(DynamicsODataClient odata) {
        this.odata = odata;
    }

    @Override
    public DynamicsPreviewResponse fetchPreview(String itemNumber, String lote) {
        long started = System.nanoTime();
        String item = trimToNull(itemNumber);
        String batch = trimToNull(lote);

        if (item == null && batch == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Indica itemNumber (código) o lote");
        }

        if (item == null) {
            item = resolveItemFromBatch(batch);
        }

        if (item == null) {
            return emptyPreview(batch, elapsedMs(started));
        }

        if (batch == null) {
            batch = resolveBatchFromItem(item, batch);
        }

        List<WarehousesOnHandV2Row> onHand = queryOnHand(item);
        List<ReleasedProductMastersRow> product = queryProduct(item);
        List<ItemBatchAttributeValuesV2Row> batchAttrs = queryBatchAttrs(item, batch);
        List<QualityOrderLineResultsRow> quality = queryQuality(batch);

        WarehousesOnHandV2Row wh = onHand.isEmpty() ? null : onHand.get(0);
        ReleasedProductMastersRow pm = product.isEmpty() ? null : product.get(0);
        QualityOrderLineResultsRow qr = quality.isEmpty() ? null : quality.get(0);

        String batchNumber = batch != null ? batch : firstBatchNumber(batchAttrs);
        String batchAttribute = joinBatchAttributes(batchAttrs);
        String batchValue = joinBatchValues(batchAttrs);

        return new DynamicsPreviewResponse(
                item,
                firstNonBlank(wh != null ? wh.ProductName : null, null),
                pm != null ? pm.ProductType : null,
                pm != null ? pm.ProductGroupId : null,
                wh != null ? wh.InventoryWarehouseId : null,
                wh != null ? wh.InventorySiteId : null,
                firstNonNull(
                        wh != null ? wh.AvailableOnHandQuantity : null,
                        wh != null ? wh.TotalAvailableQuantity : null
                ),
                wh != null ? wh.OnHandQuantity : null,
                pm != null ? pm.InventoryUnitSymbol : null,
                batchNumber,
                batchAttribute,
                batchValue,
                qr != null ? firstNonBlank(qr.TestResult, qr.QualityTestId) : null,
                qr != null ? firstNonNull(qr.ResultValue, qr.ResultInventoryQuantity) : null,
                elapsedMs(started)
        );
    }

    private String resolveItemFromBatch(String batch) {
        String filter = "BatchNumber eq '" + DynamicsODataClient.escapeOdataLiteral(batch) + "'";
        List<ItemBatchesRow> batches = odata.query(
                "ItemBatches",
                filter,
                1,
                listType(ItemBatchesRow.class)
        );
        String fromBatch = batches.stream()
                .map(r -> r.ItemNumber)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
        if (fromBatch != null) {
            return fromBatch;
        }

        List<ItemBatchAttributeValuesV2Row> rows = queryBatchAttrs(null, batch);
        return rows.stream()
                .map(r -> r.ItemNumber)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private String resolveBatchFromItem(String item, String currentBatch) {
        if (currentBatch != null) {
            return currentBatch;
        }
        String filter = "ItemNumber eq '" + DynamicsODataClient.escapeOdataLiteral(item) + "'";
        List<ItemBatchAttributeValuesV2Row> rows = odata.query(
                "ItemBatchAttributeValuesV2",
                filter,
                1,
                listType(ItemBatchAttributeValuesV2Row.class)
        );
        return rows.stream()
                .map(r -> r.ItemBatchNumber)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private List<WarehousesOnHandV2Row> queryOnHand(String item) {
        String filter = "ItemNumber eq '" + DynamicsODataClient.escapeOdataLiteral(item) + "'";
        return odata.query("WarehousesOnHandV2", filter, 1, listType(WarehousesOnHandV2Row.class));
    }

    private List<ReleasedProductMastersRow> queryProduct(String item) {
        String filter = "ItemNumber eq '" + DynamicsODataClient.escapeOdataLiteral(item) + "'";
        return odata.query("ReleasedProductMasters", filter, 1, listType(ReleasedProductMastersRow.class));
    }

    private List<ItemBatchAttributeValuesV2Row> queryBatchAttrs(String item, String batch) {
        String filter;
        if (batch != null) {
            filter = "ItemBatchNumber eq '" + DynamicsODataClient.escapeOdataLiteral(batch) + "'";
        } else if (item != null) {
            filter = "ItemNumber eq '" + DynamicsODataClient.escapeOdataLiteral(item) + "'";
        } else {
            return List.of();
        }
        return odata.query("ItemBatchAttributeValuesV2", filter, 10, listType(ItemBatchAttributeValuesV2Row.class));
    }

    private List<QualityOrderLineResultsRow> queryQuality(String batch) {
        if (batch == null) {
            return odata.query("QualityOrderLineResults", null, 1, listType(QualityOrderLineResultsRow.class));
        }
        return odata.query("QualityOrderLineResults", null, 5, listType(QualityOrderLineResultsRow.class));
    }

    private static <T> ParameterizedTypeReference<DynamicsODataClient.ODataListResponse<T>> listType(Class<T> rowType) {
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(DynamicsODataClient.ODataListResponse.class, rowType).getType()
        );
    }

    private static DynamicsPreviewResponse emptyPreview(String batch, long elapsedMs) {
        return new DynamicsPreviewResponse(
                null, null, null, null, null, null,
                null, null, null,
                batch, null, null, null, null,
                elapsedMs
        );
    }

    private static long elapsedMs(long startedNano) {
        return (System.nanoTime() - startedNano) / 1_000_000;
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static Double firstNonNull(Double a, Double b) {
        return a != null ? a : b;
    }

    private static String firstBatchNumber(List<ItemBatchAttributeValuesV2Row> rows) {
        return rows.stream()
                .map(r -> r.ItemBatchNumber)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private static String joinBatchAttributes(List<ItemBatchAttributeValuesV2Row> rows) {
        return rows.stream()
                .map(r -> r.ItemBatchAttributeId)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("; "));
    }

    private static String joinBatchValues(List<ItemBatchAttributeValuesV2Row> rows) {
        return rows.stream()
                .map(RealDynamicsPreviewService::formatBatchValue)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("; "));
    }

    private static String formatBatchValue(ItemBatchAttributeValuesV2Row r) {
        if (r.DecimalValue != null) {
            return String.valueOf(r.DecimalValue);
        }
        if (r.DateValue != null && !r.DateValue.isBlank()) {
            return r.DateValue.trim();
        }
        if (r.StringValue != null && !r.StringValue.isBlank()) {
            return r.StringValue.trim();
        }
        return null;
    }
}
