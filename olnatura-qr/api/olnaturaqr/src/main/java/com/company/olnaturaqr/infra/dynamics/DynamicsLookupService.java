package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.company.olnaturaqr.support.workflow.OperationalStatusResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
     * Busca por BatchNumber: token → ItemBatches → InventorySitesOnHand → ReleasedProductsV2
     * → InventDim/InventTrans (fecha entrada + almacenes) → QualityOrderHeaders → Estado Operativo → DTO.
     *
     * @return empty si el lote no existe en ItemBatches
     * @throws DynamicsException si falla OAuth, OData crítico, timeout o error interno
     *         (ReleasedProductsV2 y fecha de entrada se omiten ante fallo; no abortan el lookup)
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

            String unidadInventario = resolveInventoryUnit(itemNumber, accessToken);

            List<DynamicsClient.InventDimRecord> inventDims = resolveInventDims(batchNumber, accessToken);
            String fechaEntrada = resolveFechaEntrada(batchNumber, accessToken);

            Optional<DynamicsClient.QualityOrderRecord> qualityOpt =
                    dynamicsClient.findQualityOrderByItemBatch(batchNumber, accessToken);
            DynamicsClient.QualityOrderRecord quality = qualityOpt.orElse(null);

            String qualityOrderStatus = quality != null ? blankToNull(quality.qualityOrderStatus()) : null;
            String passedBatchDispositionCode = quality != null
                    ? blankToNull(quality.passedBatchDispositionCode()) : null;
            String batchDispositionCode = blankToNull(batch.batchDispositionCode());
            String statusDynamics = firstNonBlank(qualityOrderStatus, passedBatchDispositionCode);

            String qualityWarehouse = quality != null ? blankToNull(quality.warehouseId()) : null;
            String qualityLocation = quality != null ? blankToNull(quality.warehouseLocationId()) : null;

            List<String> inventLocationIds = new ArrayList<>();
            String firstInventLocation = null;
            String firstInventWms = null;
            for (DynamicsClient.InventDimRecord dim : inventDims) {
                if (dim == null) {
                    continue;
                }
                if (dim.inventLocationId() != null && !dim.inventLocationId().isBlank()) {
                    inventLocationIds.add(dim.inventLocationId());
                    if (firstInventLocation == null) {
                        firstInventLocation = dim.inventLocationId();
                        firstInventWms = dim.wmsLocationId();
                    }
                }
            }

            OperationalStatusResolver.Result op = OperationalStatusResolver.resolve(
                    inventLocationIds,
                    qualityWarehouse,
                    batchDispositionCode,
                    true
            );

            // Almacén/ubicación mostrados: el decisivo del Estado Operativo si aplica; si no, Quality / InventDim.
            String almacen = firstNonBlank(op.warehouseApplied(), qualityWarehouse, firstInventLocation);
            if (almacen == null && onHand != null) {
                almacen = blankToNull(onHand.inventorySiteId());
            }
            String ubicacion = firstNonBlank(qualityLocation, firstInventWms);

            DynamicsLookupDto dto = new DynamicsLookupDto(
                    itemNumber,
                    onHand != null ? blankToNull(onHand.productName()) : null,
                    batch.batchNumber() != null ? batch.batchNumber() : batchNumber,
                    blankToNull(batch.batchExpirationDate()),
                    onHand != null ? onHand.availableOnHandQuantity() : null,
                    unidadInventario,
                    fechaEntrada,
                    op.status(),
                    op.ruleApplied(),
                    op.statusSource(),
                    statusDynamics,
                    qualityOrderStatus,
                    passedBatchDispositionCode,
                    batchDispositionCode,
                    almacen,
                    ubicacion,
                    resolveFuente()
            );
            log.info("[EstadoOperativo] lote={} status={} rule={} warehouse={} BatchDispositionCode={} inventLocations={}",
                    dto.lote(),
                    dto.operationalStatus(),
                    dto.operationalStatusRule(),
                    nullToDash(op.warehouseApplied()),
                    nullToDash(batchDispositionCode),
                    inventLocationIds);
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

    private String resolveInventoryUnit(String itemNumber, String accessToken) {
        try {
            return dynamicsClient.findReleasedProduct(itemNumber, accessToken)
                    .map(DynamicsClient.ReleasedProductRecord::inventoryUnitSymbol)
                    .map(DynamicsLookupService::blankToNull)
                    .orElse(null);
        } catch (DynamicsException ex) {
            log.warn("DynamicsLookup: ReleasedProductsV2 omitido item={} reason={}",
                    itemNumber, ex.getClass().getSimpleName());
            return null;
        } catch (Exception ex) {
            log.warn("DynamicsLookup: ReleasedProductsV2 error item={} tipo={}",
                    itemNumber, ex.getClass().getSimpleName());
            return null;
        }
    }

    private List<DynamicsClient.InventDimRecord> resolveInventDims(String batchNumber, String accessToken) {
        try {
            List<DynamicsClient.InventDimRecord> dims = dynamicsClient.findInventDimsByBatch(batchNumber, accessToken);
            return dims != null ? dims : List.of();
        } catch (DynamicsException ex) {
            log.warn("DynamicsLookup: InventDim omitido lote={} reason={}",
                    batchNumber, ex.getClass().getSimpleName());
            return List.of();
        } catch (Exception ex) {
            log.warn("DynamicsLookup: InventDim error lote={} tipo={}",
                    batchNumber, ex.getClass().getSimpleName());
            return List.of();
        }
    }

    private String resolveFechaEntrada(String batchNumber, String accessToken) {
        try {
            return dynamicsClient.findBatchEntryDate(batchNumber, accessToken)
                    .map(DynamicsClient.BatchEntryDateRecord::datePhysical)
                    .map(DynamicsLookupService::blankToNull)
                    .orElse(null);
        } catch (DynamicsException ex) {
            log.warn("DynamicsLookup: fechaEntrada omitida lote={} reason={}",
                    batchNumber, ex.getClass().getSimpleName());
            return null;
        } catch (Exception ex) {
            log.warn("DynamicsLookup: fechaEntrada error lote={} tipo={}",
                    batchNumber, ex.getClass().getSimpleName());
            return null;
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
