package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.support.qr.LoteExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class RealDynamicsClient implements DynamicsClient {

    private static final Logger log = LoggerFactory.getLogger(RealDynamicsClient.class);
    private static final String FALLBACK_UOM = "N/A";

    private final RestClient restClient;
    private final DynamicsProperties properties;
    private final DynamicsOAuthTokenProvider tokenProvider;

    public RealDynamicsClient(DynamicsProperties properties, DynamicsOAuthTokenProvider tokenProvider) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.restClient = buildClient(properties);
    }

    @Override
    public Optional<DynamicCard> fetchByLote(String raw) {
        Optional<String> loteOpt = LoteExtractor.extract(raw);
        boolean oauthReady = properties.isOAuthConfigured()
                || (properties.getBearerToken() != null && !properties.getBearerToken().isBlank());
        log.info("Dynamics fetchByLote raw='{}' normalized='{}' authConfigured={}",
                safeForLog(raw), loteOpt.orElse("<empty>"), oauthReady);
        if (loteOpt.isEmpty()) {
            log.info("Dynamics fallback reason=LOTE_EXTRACTOR_EMPTY");
            return Optional.empty();
        }
        return fetchByLoteNormalized(loteOpt.get());
    }

    private Optional<DynamicCard> fetchByLoteNormalized(String lote) {
        log.info("Dynamics consulta por lote {}", lote);
        try {
            Optional<String> itemNumberOpt = fetchItemNumber(lote);
            if (itemNumberOpt.isEmpty()) {
                log.info("Dynamics fallback reason=BATCH_NOT_FOUND lote={}", lote);
                return Optional.empty();
            }

            String itemNumber = itemNumberOpt.get();
            log.info("Dynamics ItemNumber encontrado {}", itemNumber);
            Optional<Double> qtyOpt = fetchAvailableOnHand(itemNumber);
            if (qtyOpt.isEmpty()) {
                log.info("Dynamics fallback reason=INVENTORY_NOT_FOUND itemNumber={}", itemNumber);
                return Optional.empty();
            }
            log.info("Dynamics cantidad encontrada {} para itemNumber={}", qtyOpt.get(), itemNumber);

            return Optional.of(new DynamicCard(
                    "DESCONOCIDO",
                    qtyOpt.get(),
                    FALLBACK_UOM,
                    null,
                    "REAL_DYNAMICS"
            ));
        } catch (RestClientResponseException ex) {
            log.warn("Dynamics fallback reason=HTTP_ERROR lote={} status={} message={}",
                    lote, ex.getStatusCode().value(), ex.getMessage());
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Dynamics fallback reason=REST_CLIENT_ERROR lote={} message={}", lote, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Dynamics fallback reason=UNEXPECTED_ERROR lote={} message={}", lote, ex.toString());
            return Optional.empty();
        }
    }

    private Optional<String> fetchItemNumber(String lote) {
        String filter = "BatchNumber eq '" + escapeOdataLiteral(lote) + "'";
        log.info("Dynamics request ItemBatches lote={}", lote);
        ResponseEntity<ItemBatchesResponse> responseEntity = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/ItemBatches")
                        .queryParam("$filter", filter)
                        .queryParam("$top", 1)
                        .build())
                .headers(headers -> applyAuth(headers::setBearerAuth))
                .retrieve()
                .toEntity(ItemBatchesResponse.class);
        log.info("Dynamics response ItemBatches status={}", responseEntity.getStatusCode().value());
        ItemBatchesResponse response = responseEntity.getBody();

        if (response == null || response.value == null || response.value.isEmpty()) {
            return Optional.empty();
        }
        ItemBatchesRow first = response.value.get(0);
        if (first == null || first.ItemNumber == null || first.ItemNumber.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(first.ItemNumber.trim());
    }

    private Optional<Double> fetchAvailableOnHand(String itemNumber) {
        String filter = "ItemNumber eq '" + escapeOdataLiteral(itemNumber) + "'";
        log.info("Dynamics request InventorySitesOnHandV2 itemNumber={}", itemNumber);
        ResponseEntity<InventoryOnHandResponse> responseEntity = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/InventorySitesOnHandV2")
                        .queryParam("$filter", filter)
                        .queryParam("$top", 1)
                        .build())
                .headers(headers -> applyAuth(headers::setBearerAuth))
                .retrieve()
                .toEntity(InventoryOnHandResponse.class);
        log.info("Dynamics response InventorySitesOnHandV2 status={}", responseEntity.getStatusCode().value());
        InventoryOnHandResponse response = responseEntity.getBody();

        if (response == null || response.value == null || response.value.isEmpty()) {
            return Optional.empty();
        }
        InventoryOnHandRow first = response.value.get(0);
        if (first == null || first.AvailableOnHandQuantity == null) {
            return Optional.empty();
        }
        return Optional.of(first.AvailableOnHandQuantity);
    }

    private void applyAuth(java.util.function.Consumer<String> bearerSetter) {
        bearerSetter.accept(tokenProvider.getAccessToken());
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

    private String escapeOdataLiteral(String value) {
        return value.replace("'", "''");
    }

    private String safeForLog(String value) {
        if (value == null) {
            return "<null>";
        }
        String v = value.trim();
        if (v.length() <= 120) {
            return v;
        }
        return v.substring(0, 120) + "...";
    }

    private static class ItemBatchesResponse {
        public List<ItemBatchesRow> value;
    }

    private static class ItemBatchesRow {
        public String ItemNumber;
    }

    private static class InventoryOnHandResponse {
        public List<InventoryOnHandRow> value;
    }

    private static class InventoryOnHandRow {
        public Double AvailableOnHandQuantity;
    }
}
