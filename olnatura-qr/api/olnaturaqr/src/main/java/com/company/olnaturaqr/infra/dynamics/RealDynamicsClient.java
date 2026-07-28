package com.company.olnaturaqr.infra.dynamics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
                            .queryParam("$select", "ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode")
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
            String disposition = row.BatchDispositionCode;
            return Optional.of(new ItemBatchRecord(
                    row.ItemNumber.trim(),
                    row.BatchNumber != null ? row.BatchNumber.trim() : batchNumber,
                    row.BatchExpirationDate,
                    disposition != null && !disposition.isBlank() ? disposition.trim() : null
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

    @Override
    public Optional<BatchEntryDateRecord> findBatchEntryDate(String batchNumber, String accessToken) {
        // Logs temporales de prueba (fecha de entrada).
        log.info("[FechaEntrada] Lote consultado={}", batchNumber);

        List<InventDimRecord> dims = findInventDimsByBatch(batchNumber, accessToken);
        List<String> inventDimIds = dims.stream()
                .map(InventDimRecord::inventDimId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        log.info("[FechaEntrada] inventDimId encontrados={} lote={}", inventDimIds.size(), batchNumber);

        if (inventDimIds.isEmpty()) {
            log.info("[FechaEntrada] No se encontró fecha válida (sin inventDimId) lote={}", batchNumber);
            return Optional.empty();
        }

        Instant minPhysical = null;
        String minRaw = null;
        int validTotal = 0;

        for (String inventDimId : inventDimIds) {
            log.info("[FechaEntrada] InventDimId consultado={} lote={}", inventDimId, batchNumber);
            List<InventTransRow> rows;
            try {
                rows = findInventTransByDim(inventDimId, accessToken);
            } catch (DynamicsException ex) {
                // Continuar con los demás inventDimId si uno falla o no tiene datos útiles.
                log.warn("[FechaEntrada] InventTrans omitido inventDimId={} lote={} reason={}",
                        inventDimId, batchNumber, ex.getClass().getSimpleName());
                log.info("[FechaEntrada] movimientos obtenidos=0 (error) inventDimId={} lote={}",
                        inventDimId, batchNumber);
                continue;
            }

            log.info("[FechaEntrada] movimientos obtenidos={} inventDimId={} lote={}",
                    rows.size(), inventDimId, batchNumber);

            int validForDim = 0;
            for (InventTransRow row : rows) {
                if (row == null || !isEntryReceiptStatus(row.StatusReceipt)) {
                    continue;
                }
                if (!isValidPhysicalDate(row.DatePhysical)) {
                    continue;
                }
                Instant instant = parseDatePhysical(row.DatePhysical);
                if (instant == null) {
                    continue;
                }
                validForDim++;
                validTotal++;
                if (minPhysical == null || instant.isBefore(minPhysical)) {
                    minPhysical = instant;
                    minRaw = row.DatePhysical.trim();
                }
            }
            log.info("[FechaEntrada] movimientos válidos (Received|Purchased)={} inventDimId={} lote={}",
                    validForDim, inventDimId, batchNumber);
        }

        log.info("[FechaEntrada] movimientos válidos totales={} lote={}", validTotal, batchNumber);

        if (minRaw == null) {
            log.info("[FechaEntrada] No se encontró fecha válida lote={}", batchNumber);
            return Optional.empty();
        }
        log.info("[FechaEntrada] Fecha de entrada seleccionada={} lote={}", minRaw, batchNumber);
        return Optional.of(new BatchEntryDateRecord(minRaw));
    }

    @Override
    public List<InventDimRecord> findInventDimsByBatch(String batchNumber, String accessToken) {
        String filter = "inventBatchId eq '" + escapeOdataLiteral(batchNumber) + "'";
        log.debug("Dynamics OData GET InventDimBiEntities lote={}", batchNumber);
        try {
            ResponseEntity<InventDimResponse> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/InventDimBiEntities")
                            .queryParam("$filter", filter)
                            .queryParam("$select", "inventDimId,inventBatchId,InventLocationId,wMSLocationId")
                            .queryParam("$top", 50)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(InventDimResponse.class);
            log.debug("Dynamics OData InventDimBiEntities HTTP {}", responseEntity.getStatusCode().value());

            InventDimResponse body = responseEntity.getBody();
            if (body == null || body.value == null || body.value.isEmpty()) {
                return List.of();
            }
            List<InventDimRecord> out = new ArrayList<>();
            for (InventDimRow row : body.value) {
                if (row == null || row.inventDimId == null || row.inventDimId.isBlank()) {
                    continue;
                }
                out.add(new InventDimRecord(
                        row.inventDimId.trim(),
                        blankToNull(row.InventLocationId),
                        blankToNull(row.wMSLocationId)
                ));
            }
            return List.copyOf(out);
        } catch (RestClientException ex) {
            log.warn("Dynamics OData InventDimBiEntities falló lote={} tipo={}",
                    batchNumber, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOData("InventDimBiEntities", batchNumber, ex);
        }
    }

    private List<InventTransRow> findInventTransByDim(String inventDimId, String accessToken) {
        String filter = "inventDimId eq '" + escapeOdataLiteral(inventDimId) + "'";
        log.debug("Dynamics OData GET InventTransCDSEntities inventDimId={}", inventDimId);
        try {
            ResponseEntity<InventTransResponse> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/InventTransCDSEntities")
                            .queryParam("$filter", filter)
                            .queryParam("$select", "inventDimId,StatusReceipt,DatePhysical")
                            .queryParam("$top", 100)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(InventTransResponse.class);
            log.debug("Dynamics OData InventTransCDSEntities HTTP {}", responseEntity.getStatusCode().value());

            InventTransResponse body = responseEntity.getBody();
            if (body == null || body.value == null || body.value.isEmpty()) {
                return List.of();
            }
            return body.value;
        } catch (RestClientException ex) {
            log.warn("Dynamics OData InventTransCDSEntities falló inventDimId={} tipo={}",
                    inventDimId, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOData("InventTransCDSEntities", inventDimId, ex);
        }
    }

    /** Recibo de entrada válido: Received (p.ej. MPS) o Purchased (p.ej. MEM). */
    private static boolean isEntryReceiptStatus(String statusReceipt) {
        if (statusReceipt == null || statusReceipt.isBlank()) {
            return false;
        }
        String status = statusReceipt.trim();
        return "Received".equalsIgnoreCase(status) || "Purchased".equalsIgnoreCase(status);
    }

    private static boolean isValidPhysicalDate(String datePhysical) {
        if (datePhysical == null || datePhysical.isBlank()) {
            return false;
        }
        String trimmed = datePhysical.trim();
        if (trimmed.startsWith("1900-01-01")) {
            return false;
        }
        LocalDate day = toLocalDateUtc(trimmed);
        return day != null && !LocalDate.of(1900, 1, 1).equals(day);
    }

    private static Instant parseDatePhysical(String datePhysical) {
        if (datePhysical == null || datePhysical.isBlank()) {
            return null;
        }
        String trimmed = datePhysical.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Continuar con LocalDate
        }
        LocalDate day = toLocalDateUtc(trimmed);
        if (day == null) {
            return null;
        }
        return day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static LocalDate toLocalDateUtc(String datePhysical) {
        try {
            if (datePhysical.length() >= 10 && datePhysical.charAt(4) == '-') {
                return LocalDate.parse(datePhysical.substring(0, 10));
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static class ItemBatchesResponse {
        public List<ItemBatchesRow> value;
    }

    private static class ItemBatchesRow {
        public String ItemNumber;
        public String BatchNumber;
        public String BatchExpirationDate;
        public String BatchDispositionCode;
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

    private static class InventDimResponse {
        public List<InventDimRow> value;
    }

    private static class InventDimRow {
        public String inventDimId;
        public String inventBatchId;
        public String InventLocationId;
        public String wMSLocationId;
    }

    private static class InventTransResponse {
        public List<InventTransRow> value;
    }

    private static class InventTransRow {
        public String inventDimId;
        public String StatusReceipt;
        public String DatePhysical;
    }
}
