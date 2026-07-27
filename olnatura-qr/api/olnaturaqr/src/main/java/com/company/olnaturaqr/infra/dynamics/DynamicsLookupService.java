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
     * Busca por BatchNumber: token → ItemBatches → InventorySitesOnHand → ReleasedProductsV2
     * → InventDim/InventTrans (fecha entrada) → QualityOrderHeaders → DTO.
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

            // Unidad de inventario: solo InventoryUnitSymbol. Si falla, se omite (solo número).
            String unidadInventario = resolveInventoryUnit(itemNumber, accessToken);

            // Fecha de entrada: InventDimBiEntities → InventTrans (MIN DatePhysical Received|Purchased).
            String fechaEntrada = resolveFechaEntrada(batchNumber, accessToken);

            Optional<DynamicsClient.QualityOrderRecord> qualityOpt =
                    dynamicsClient.findQualityOrderByItemBatch(batchNumber, accessToken);
            DynamicsClient.QualityOrderRecord quality = qualityOpt.orElse(null);

            String qualityOrderStatus = quality != null ? blankToNull(quality.qualityOrderStatus()) : null;
            String passedBatchDispositionCode = quality != null
                    ? blankToNull(quality.passedBatchDispositionCode()) : null;
            String batchDispositionCode = blankToNull(batch.batchDispositionCode());
            // Resumen legado (informativo): QualityOrderStatus; no sincroniza estado QR.
            String statusDynamics = firstNonBlank(qualityOrderStatus, passedBatchDispositionCode);

            String almacen = null;
            String ubicacion = null;
            if (quality != null) {
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
                    unidadInventario,
                    fechaEntrada,
                    statusDynamics,
                    qualityOrderStatus,
                    passedBatchDispositionCode,
                    batchDispositionCode,
                    almacen,
                    ubicacion,
                    resolveFuente()
            );
            // Log temporal diagnóstico estado QR vs Dynamics (estado QR se completa en QrQueryService).
            log.info("[EstadoDiag] lote={} QualityOrderStatus={} PassedBatchDispositionCode={} BatchDispositionCode={}",
                    dto.lote(),
                    nullToDash(qualityOrderStatus),
                    nullToDash(passedBatchDispositionCode),
                    nullToDash(batchDispositionCode));
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

    /**
     * Consulta ReleasedProductsV2 por ItemNumber.
     * Ante fallo o sin unidad: null (no interrumpe el lookup).
     */
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

    /**
     * Fecha de entrada vía InventDimBiEntities + InventTransCDSEntities.
     * Ante fallo o sin movimientos Received: null (no interrumpe el lookup).
     */
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

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
