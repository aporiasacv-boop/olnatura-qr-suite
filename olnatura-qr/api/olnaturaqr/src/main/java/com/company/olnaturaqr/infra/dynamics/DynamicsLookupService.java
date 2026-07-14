package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.support.qr.LoteExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orquesta token OAuth + consultas OData y construye {@link DynamicsLookupDto}.
 * {@link DynamicsClient} permanece como cliente HTTP únicamente.
 */
@Service
public class DynamicsLookupService {

    private static final Logger log = LoggerFactory.getLogger(DynamicsLookupService.class);
    private static final String FUENTE_REAL = "REAL_DYNAMICS";
    private static final String FUENTE_MOCK = "MOCK_DYNAMICS";

    private final DynamicsClient dynamicsClient;
    private final DynamicsProperties properties;
    private final ObjectProvider<DynamicsOAuthTokenClient> oauthTokenClient;

    public DynamicsLookupService(
            DynamicsClient dynamicsClient,
            DynamicsProperties properties,
            ObjectProvider<DynamicsOAuthTokenClient> oauthTokenClient
    ) {
        this.dynamicsClient = dynamicsClient;
        this.properties = properties;
        this.oauthTokenClient = oauthTokenClient;
    }

    /**
     * Busca por BatchNumber: token nuevo → ItemBatches → InventorySitesOnHand → QualityOrderHeaders → DTO.
     *
     * @return empty si el lote no existe en ItemBatches
     * @throws DynamicsException si falla OAuth, OData, timeout o error interno
     */
    public Optional<DynamicsLookupDto> lookupByBatchNumber(String rawBatchNumber) {
        Optional<String> loteOpt = LoteExtractor.extract(rawBatchNumber);
        if (loteOpt.isEmpty()) {
            log.debug("DynamicsLookup: identificador vacío tras extract");
            return Optional.empty();
        }
        String batchNumber = loteOpt.get();

        String accessToken = requestTokenForLookup();
        try {
            return executeLookup(batchNumber, accessToken);
        } finally {
            accessToken = null;
        }
    }

    private Optional<DynamicsLookupDto> executeLookup(String batchNumber, String accessToken) {
        try {
            Optional<DynamicsClient.ItemBatchRecord> batchOpt =
                    dynamicsClient.findItemBatch(batchNumber, accessToken);
            if (batchOpt.isEmpty()) {
                log.debug("DynamicsLookup: ItemBatches sin filas lote={}", batchNumber);
                return Optional.empty();
            }
            DynamicsClient.ItemBatchRecord batch = batchOpt.get();
            String itemNumber = batch.itemNumber();

            Optional<DynamicsClient.InventoryOnHandRecord> onHandOpt =
                    dynamicsClient.findInventorySitesOnHand(itemNumber, accessToken);
            DynamicsClient.InventoryOnHandRecord onHand = onHandOpt.orElse(null);

            Optional<DynamicsClient.QualityOrderRecord> qualityOpt =
                    dynamicsClient.findQualityOrderByItemBatch(batchNumber, accessToken);
            DynamicsClient.QualityOrderRecord quality = qualityOpt.orElse(null);

            String statusDynamics = null;
            String almacen = null;
            String ubicacion = null;
            if (quality != null) {
                statusDynamics = firstNonBlank(quality.qualityOrderStatus(), quality.passedBatchDispositionCode());
                almacen = blankToNull(quality.warehouseId());
                ubicacion = blankToNull(quality.warehouseLocationId());
            }
            if (almacen == null && onHand != null) {
                almacen = blankToNull(onHand.inventorySiteId());
            }

            DynamicsLookupDto dto = new DynamicsLookupDto(
                    itemNumber,
                    onHand != null ? blankToNull(onHand.productName()) : null,
                    batch.batchNumber() != null ? batch.batchNumber() : batchNumber,
                    blankToNull(batch.batchExpirationDate()),
                    onHand != null ? onHand.availableOnHandQuantity() : null,
                    statusDynamics,
                    almacen,
                    ubicacion,
                    resolveFuente()
            );
            log.debug("DynamicsLookup OK lote={} fuente={}", dto.lote(), dto.fuente());
            return Optional.of(dto);
        } catch (DynamicsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("DynamicsLookup error interno lote={} tipo={}",
                    batchNumber, ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.unexpected("lookup:" + batchNumber, ex);
        }
    }

    private String requestTokenForLookup() {
        DynamicsOAuthTokenClient oauth = oauthTokenClient.getIfAvailable();
        if (oauth != null) {
            return oauth.requestAccessToken();
        }
        log.debug("DynamicsLookup: modo={} sin OAuth (mock)", properties.getMode());
        return "MOCK_TOKEN";
    }

    private String resolveFuente() {
        if ("mock".equalsIgnoreCase(properties.getMode())) {
            return FUENTE_MOCK;
        }
        return FUENTE_REAL;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
