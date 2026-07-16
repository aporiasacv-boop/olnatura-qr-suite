package com.company.olnaturaqr.infra.dynamics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Cliente HTTP puro hacia Dynamics OData. No obtiene tokens ni arma DTOs de negocio.
 */
@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class RealDynamicsClient implements DynamicsClient {

    private static final Logger log = LoggerFactory.getLogger(RealDynamicsClient.class);

    private final RestClient restClient;

    public RealDynamicsClient(DynamicsProperties properties) {
        this.restClient = buildClient(properties);
    }

    @Override
    public Optional<ItemBatchRecord> findItemBatch(String batchNumber, String accessToken) {
        String filter = "BatchNumber eq '" + escapeOdataLiteral(batchNumber) + "'";
        log.debug("Dynamics OData GET ItemBatches lote={}", batchNumber);
        try {
            ResponseEntity<ItemBatchesResponse> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/ItemBatches")
                            .queryParam("$filter", filter)
                            .queryParam("$select", "ItemNumber,BatchNumber,BatchExpirationDate")
                            .queryParam("$top", 1)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(ItemBatchesResponse.class);
            log.debug("Dynamics OData ItemBatches HTTP {}", responseEntity.getStatusCode().value());

            ItemBatchesResponse body = responseEntity.getBody();
            if (body == null || body.value == null || body.value.isEmpty() || body.value.get(0) == null) {
                return Optional.empty();
            }
            ItemBatchesRow row = body.value.get(0);
            if (row.ItemNumber == null || row.ItemNumber.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ItemBatchRecord(
                    row.ItemNumber.trim(),
                    row.BatchNumber != null ? row.BatchNumber.trim() : batchNumber,
                    row.BatchExpirationDate
            ));
        } catch (RestClientException ex) {
            log.warn("Dynamics OData ItemBatches falló lote={} tipo={}",
                    batchNumber, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOData("ItemBatches", batchNumber, ex);
        }
    }

    @Override
    public Optional<InventoryOnHandRecord> findInventorySitesOnHand(String itemNumber, String accessToken) {
        String filter = "ItemNumber eq '" + escapeOdataLiteral(itemNumber) + "'";
        log.debug("Dynamics OData GET InventorySitesOnHand item={}", itemNumber);
        try {
            ResponseEntity<InventoryOnHandResponse> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/InventorySitesOnHand")
                            .queryParam("$filter", filter)
                            .queryParam("$select", "ItemNumber,ProductName,AvailableOnHandQuantity,InventorySiteId")
                            .queryParam("$top", 1)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(InventoryOnHandResponse.class);
            log.debug("Dynamics OData InventorySitesOnHand HTTP {}", responseEntity.getStatusCode().value());

            InventoryOnHandResponse body = responseEntity.getBody();
            if (body == null || body.value == null || body.value.isEmpty() || body.value.get(0) == null) {
                return Optional.empty();
            }
            InventoryOnHandRow row = body.value.get(0);
            return Optional.of(new InventoryOnHandRecord(
                    row.ItemNumber,
                    row.ProductName,
                    row.AvailableOnHandQuantity,
                    row.InventorySiteId
            ));
        } catch (RestClientException ex) {
            log.warn("Dynamics OData InventorySitesOnHand falló item={} tipo={}",
                    itemNumber, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOData("InventorySitesOnHand", itemNumber, ex);
        }
    }

    @Override
    public Optional<QualityOrderRecord> findQualityOrderByItemBatch(String itemBatchNumber, String accessToken) {
        String filter = "ItemBatchNumber eq '" + escapeOdataLiteral(itemBatchNumber) + "'";
        log.debug("Dynamics OData GET QualityOrderHeaders lote={}", itemBatchNumber);
        try {
            ResponseEntity<QualityOrderResponse> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/QualityOrderHeaders")
                            .queryParam("$filter", filter)
                            .queryParam("$select",
                                    "ItemBatchNumber,ItemNumber,QualityOrderStatus,PassedBatchDispositionCode,WarehouseId,WarehouseLocationId")
                            .queryParam("$top", 1)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(QualityOrderResponse.class);
            log.debug("Dynamics OData QualityOrderHeaders HTTP {}", responseEntity.getStatusCode().value());

            QualityOrderResponse body = responseEntity.getBody();
            if (body == null || body.value == null || body.value.isEmpty() || body.value.get(0) == null) {
                return Optional.empty();
            }
            QualityOrderRow row = body.value.get(0);
            return Optional.of(new QualityOrderRecord(
                    row.ItemBatchNumber,
                    row.ItemNumber,
                    row.QualityOrderStatus,
                    row.PassedBatchDispositionCode,
                    row.WarehouseId,
                    row.WarehouseLocationId
            ));
        } catch (RestClientException ex) {
            log.warn("Dynamics OData QualityOrderHeaders falló lote={} tipo={}",
                    itemBatchNumber, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOData("QualityOrderHeaders", itemBatchNumber, ex);
        }
    }

    @Override
    public Optional<ReleasedProductRecord> findReleasedProduct(String itemNumber, String accessToken) {
        String filter = "ItemNumber eq '" + escapeOdataLiteral(itemNumber) + "'";
        log.debug("Dynamics OData GET ReleasedProductsV2 item={}", itemNumber);
        try {
            ResponseEntity<ReleasedProductsResponse> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/ReleasedProductsV2")
                            .queryParam("$filter", filter)
                            .queryParam("$select", "ItemNumber,InventoryUnitSymbol")
                            .queryParam("$top", 1)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(ReleasedProductsResponse.class);
            log.debug("Dynamics OData ReleasedProductsV2 HTTP {}", responseEntity.getStatusCode().value());

            ReleasedProductsResponse body = responseEntity.getBody();
            if (body == null || body.value == null || body.value.isEmpty() || body.value.get(0) == null) {
                return Optional.empty();
            }
            ReleasedProductsRow row = body.value.get(0);
            String unit = row.InventoryUnitSymbol;
            if (unit == null || unit.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ReleasedProductRecord(
                    row.ItemNumber != null ? row.ItemNumber.trim() : itemNumber,
                    unit.trim()
            ));
        } catch (RestClientException ex) {
            log.warn("Dynamics OData ReleasedProductsV2 falló item={} tipo={}",
                    itemNumber, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOData("ReleasedProductsV2", itemNumber, ex);
        }
    }

    private RestClient buildClient(DynamicsProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) props.getReadTimeout().toMillis());
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static String escapeOdataLiteral(String value) {
        return value.replace("'", "''");
    }

    private static class ItemBatchesResponse {
        public List<ItemBatchesRow> value;
    }

    private static class ItemBatchesRow {
        public String ItemNumber;
        public String BatchNumber;
        public String BatchExpirationDate;
    }

    private static class InventoryOnHandResponse {
        public List<InventoryOnHandRow> value;
    }

    private static class InventoryOnHandRow {
        public String ItemNumber;
        public String ProductName;
        public Double AvailableOnHandQuantity;
        public String InventorySiteId;
    }

    private static class QualityOrderResponse {
        public List<QualityOrderRow> value;
    }

    private static class QualityOrderRow {
        public String ItemBatchNumber;
        public String ItemNumber;
        public String QualityOrderStatus;
        public String PassedBatchDispositionCode;
        public String WarehouseId;
        public String WarehouseLocationId;
    }

    private static class ReleasedProductsResponse {
        public List<ReleasedProductsRow> value;
    }

    private static class ReleasedProductsRow {
        public String ItemNumber;
        public String InventoryUnitSymbol;
    }
}
