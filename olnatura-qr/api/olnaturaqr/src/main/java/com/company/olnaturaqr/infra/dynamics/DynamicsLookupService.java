package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.company.olnaturaqr.support.workflow.OperationalStatusResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class DynamicsLookupService {

    private static final Logger log = LoggerFactory.getLogger(DynamicsLookupService.class);
    private static final String FUENTE_REAL = "REAL_DYNAMICS";
    private static final String FUENTE_MOCK = "MOCK_DYNAMICS";

    private final DynamicsClient dynamicsClient;
    private final DynamicsProperties properties;
    private final ObjectProvider<DynamicsOAuthTokenClient> oauthTokenClient;
    private Clock clock = Clock.systemDefaultZone();

    public DynamicsLookupService(
            DynamicsClient dynamicsClient,
            DynamicsProperties properties,
            ObjectProvider<DynamicsOAuthTokenClient> oauthTokenClient
    ) {
        this.dynamicsClient = dynamicsClient;
        this.properties = properties;
        this.oauthTokenClient = oauthTokenClient;
    }

    
    public Optional<DynamicsLookupDto> lookupByBatchNumber(String rawBatchNumber) {
        return lookupByBatchNumber(rawBatchNumber, null);
    }

    /**
     * @param itemNumberHint artículo de la etiqueta ({@code QrLabel.codigo}) si se conoce. El número de lote
     *                       no es único entre artículos; con el artículo se evita tomar el lote de otro producto.
     */
    public Optional<DynamicsLookupDto> lookupByBatchNumber(String rawBatchNumber, String itemNumberHint) {
        Optional<String> loteOpt = LoteExtractor.extract(rawBatchNumber);
        if (loteOpt.isEmpty()) {
            log.debug("DynamicsLookup: identificador vacío tras extract");
            return Optional.empty();
        }
        String batchNumber = loteOpt.get();

        String accessToken = requestTokenForLookup();
        try {
            return executeLookup(batchNumber, blankToNull(itemNumberHint), accessToken);
        } finally {
            accessToken = null;
        }
    }

    /** Solo para pruebas: fija la fecha con la que se evalúa la caducidad. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    private Optional<DynamicsLookupDto> executeLookup(String batchNumber, String itemNumberHint, String accessToken) {
        try {
            List<DynamicsClient.ItemBatchRecord> candidates = dynamicsClient.findItemBatches(batchNumber, accessToken);
            if (candidates == null || candidates.isEmpty()) {
                log.debug("DynamicsLookup: ItemBatches sin filas lote={}", batchNumber);
                return Optional.empty();
            }
            ItemSelection selection = selectItem(candidates, itemNumberHint, batchNumber, accessToken);
            DynamicsClient.ItemBatchRecord batch = selection.batch();
            String itemNumber = batch.itemNumber();

            Optional<DynamicsClient.InventoryOnHandRecord> onHandOpt =
                    dynamicsClient.findInventorySitesOnHand(itemNumber, accessToken);
            DynamicsClient.InventoryOnHandRecord onHand = onHandOpt.orElse(null);

            String unidadInventario = resolveInventoryUnit(itemNumber, accessToken);

            List<DynamicsClient.BatchOnHandRecord> batchOnHand = selection.onHand() != null
                    ? selection.onHand()
                    : resolveBatchOnHand(itemNumber, batchNumber, accessToken);
            List<OperationalStatusResolver.StockLine> stock = toStockLines(batchOnHand);
            Double cantidadLote = sumQuantity(stock);

            BatchEntryInfo entryInfo = resolveBatchEntry(batchNumber, accessToken);
            String fechaEntrada = entryInfo.fechaEntrada();
            Double cantidadRecibida = entryInfo.cantidadRecibida();

            Optional<DynamicsClient.QualityOrderRecord> qualityOpt =
                    dynamicsClient.findLatestQualityOrder(itemNumber, batchNumber, accessToken);
            DynamicsClient.QualityOrderRecord quality = qualityOpt.orElse(null);

            String qualityOrderStatus = quality != null ? blankToNull(quality.qualityOrderStatus()) : null;
            String passedBatchDispositionCode = quality != null
                    ? blankToNull(quality.passedBatchDispositionCode()) : null;
            String batchDispositionCode = blankToNull(batch.batchDispositionCode());
            String statusDynamics = firstNonBlank(qualityOrderStatus, passedBatchDispositionCode);

            String qualityWarehouse = quality != null ? blankToNull(quality.warehouseId()) : null;
            String qualityLocation = quality != null ? blankToNull(quality.warehouseLocationId()) : null;

            List<String> stockWarehouses = OperationalStatusResolver.warehousesWithStock(stock);
            String firstStockWarehouse = stockWarehouses.isEmpty() ? null : stockWarehouses.get(0);
            String firstStockLocation = stock.stream()
                    .filter(s -> Math.abs(s.quantity()) > 1e-9)
                    .map(OperationalStatusResolver.StockLine::locationId)
                    .filter(l -> l != null && !l.isBlank())
                    .findFirst()
                    .orElse(null);

            // batchDispositionCode se sigue leyendo para diagnóstico/DTO; la decisión EO usa la existencia del lote,
            // la orden de calidad más reciente y la caducidad. El almacén de la orden de calidad no es existencia.
            OperationalStatusResolver.Result op = OperationalStatusResolver.resolve(
                    stock,
                    qualityOrderStatus,
                    quality != null ? quality.validatedDateTime() : null,
                    batch.batchExpirationDate(),
                    LocalDate.now(clock),
                    true
            );

            String almacen = firstNonBlank(op.warehouseApplied(), firstStockWarehouse, qualityWarehouse);
            if (almacen == null && onHand != null) {
                almacen = blankToNull(onHand.inventorySiteId());
            }
            String ubicacion = firstNonBlank(firstStockLocation, qualityLocation);

            String fechaLiberacion = null;
            String liberadoPor = null;
            if ("APROBADO".equalsIgnoreCase(op.status()) && quality != null) {
                fechaLiberacion = sanitizeValidatedDateTime(quality.validatedDateTime());
                liberadoPor = blankToNull(quality.validatingPersonnelNumber());
            }

            List<String> warehouses = new ArrayList<>(stockWarehouses);
            OperationalStatusResolver.StockSplit split = OperationalStatusResolver.split(stock);

            DynamicsLookupDto dto = new DynamicsLookupDto(
                    itemNumber,
                    onHand != null ? blankToNull(onHand.productName()) : null,
                    batch.batchNumber() != null ? batch.batchNumber() : batchNumber,
                    blankToNull(batch.batchExpirationDate()),
                    cantidadLote,
                    cantidadRecibida,
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
                    resolveFuente(),
                    fechaLiberacion,
                    liberadoPor,
                    List.copyOf(warehouses),
                    split.usableQuantity(),
                    split.rejectedQuantity(),
                    split.rejectedPlaces()
            );
            log.info("[EstadoOperativo] lote={} item={} status={} rule={} warehouse={} BatchDispositionCode={} existencia={}",
                    dto.lote(),
                    itemNumber,
                    dto.operationalStatus(),
                    dto.operationalStatusRule(),
                    nullToDash(op.warehouseApplied()),
                    nullToDash(batchDispositionCode),
                    stockWarehouses);
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

    private record ItemSelection(
            DynamicsClient.ItemBatchRecord batch,
            /** Existencia ya consultada al desempatar; null si no se consultó. */
            List<DynamicsClient.BatchOnHandRecord> onHand
    ) {}

    /**
     * Elige el artículo del lote: el de la etiqueta si coincide; si solo hay uno, ese; si hay varios,
     * el primero (por ItemNumber) que tenga existencia distinta de cero.
     */
    private ItemSelection selectItem(
            List<DynamicsClient.ItemBatchRecord> candidates,
            String itemNumberHint,
            String batchNumber,
            String accessToken
    ) {
        if (itemNumberHint != null) {
            for (DynamicsClient.ItemBatchRecord c : candidates) {
                if (itemNumberHint.equalsIgnoreCase(c.itemNumber())) {
                    return new ItemSelection(c, null);
                }
            }
            log.warn("DynamicsLookup: el artículo de la etiqueta no tiene ese lote item={} lote={} candidatos={}",
                    itemNumberHint, batchNumber, candidates.stream().map(DynamicsClient.ItemBatchRecord::itemNumber).toList());
        }
        if (candidates.size() == 1) {
            return new ItemSelection(candidates.get(0), null);
        }
        log.warn("DynamicsLookup: lote con varios artículos lote={} candidatos={}",
                batchNumber, candidates.stream().map(DynamicsClient.ItemBatchRecord::itemNumber).toList());
        for (DynamicsClient.ItemBatchRecord c : candidates) {
            List<DynamicsClient.BatchOnHandRecord> onHand = resolveBatchOnHand(c.itemNumber(), batchNumber, accessToken);
            if (sumQuantity(toStockLines(onHand)) != null) {
                return new ItemSelection(c, onHand);
            }
        }
        return new ItemSelection(candidates.get(0), null);
    }

    private List<DynamicsClient.BatchOnHandRecord> resolveBatchOnHand(
            String itemNumber,
            String batchNumber,
            String accessToken
    ) {
        try {
            List<DynamicsClient.BatchOnHandRecord> rows = dynamicsClient.findBatchOnHand(itemNumber, batchNumber, accessToken);
            return rows != null ? rows : List.of();
        } catch (DynamicsException ex) {
            log.warn("DynamicsLookup: existencia del lote omitida item={} lote={} reason={}",
                    itemNumber, batchNumber, ex.getClass().getSimpleName());
            return List.of();
        } catch (Exception ex) {
            log.warn("DynamicsLookup: existencia del lote error item={} lote={} tipo={}",
                    itemNumber, batchNumber, ex.getClass().getSimpleName());
            return List.of();
        }
    }

    private static List<OperationalStatusResolver.StockLine> toStockLines(List<DynamicsClient.BatchOnHandRecord> rows) {
        List<OperationalStatusResolver.StockLine> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (DynamicsClient.BatchOnHandRecord r : rows) {
            if (r == null || r.warehouseId() == null || r.availablePhysicalQuantity() == null) {
                continue;
            }
            out.add(new OperationalStatusResolver.StockLine(
                    r.warehouseId(), r.locationId(), r.availablePhysicalQuantity()));
        }
        return out;
    }

    /** Suma de la existencia del lote; null si no hay ninguna cantidad distinta de cero. */
    private static Double sumQuantity(List<OperationalStatusResolver.StockLine> stock) {
        double sum = 0d;
        boolean any = false;
        for (OperationalStatusResolver.StockLine s : stock) {
            if (Math.abs(s.quantity()) > 1e-9) {
                sum += s.quantity();
                any = true;
            }
        }
        return any ? sum : null;
    }

    private record BatchEntryInfo(String fechaEntrada, Double cantidadRecibida) {}

    private BatchEntryInfo resolveBatchEntry(String batchNumber, String accessToken) {
        try {
            return dynamicsClient.findBatchEntryDate(batchNumber, accessToken)
                    .map(r -> new BatchEntryInfo(
                            blankToNull(r.datePhysical()),
                            r.receivedQuantity()
                    ))
                    .orElse(new BatchEntryInfo(null, null));
        } catch (DynamicsException ex) {
            log.warn("DynamicsLookup: fechaEntrada/cantidadRecibida omitida lote={} reason={}",
                    batchNumber, ex.getClass().getSimpleName());
            return new BatchEntryInfo(null, null);
        } catch (Exception ex) {
            log.warn("DynamicsLookup: fechaEntrada/cantidadRecibida error lote={} tipo={}",
                    batchNumber, ex.getClass().getSimpleName());
            return new BatchEntryInfo(null, null);
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

    
    private static String sanitizeValidatedDateTime(String value) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.startsWith("1900-01-01")) {
            return null;
        }
        return trimmed;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
