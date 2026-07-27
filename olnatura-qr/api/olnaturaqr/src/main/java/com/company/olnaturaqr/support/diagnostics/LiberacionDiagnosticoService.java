package com.company.olnaturaqr.support.diagnostics;

import com.company.olnaturaqr.infra.dynamics.DynamicsClient;
import com.company.olnaturaqr.infra.dynamics.DynamicsOAuthTokenClient;
import com.company.olnaturaqr.infra.dynamics.DynamicsProperties;
import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Consulta Dynamics (ItemBatches + QualityOrderHeaders) y guarda evidencia JSON.
 * No modifica estado QR ni sincroniza.
 */
@Service
public class LiberacionDiagnosticoService {

    private static final Logger log = LoggerFactory.getLogger(LiberacionDiagnosticoService.class);

    private final DynamicsClient dynamicsClient;
    private final DynamicsProperties properties;
    private final ObjectProvider<DynamicsOAuthTokenClient> oauthTokenClient;
    private final Path diagnosticsDir;
    private final ObjectMapper mapper;

    public LiberacionDiagnosticoService(
            DynamicsClient dynamicsClient,
            DynamicsProperties properties,
            ObjectProvider<DynamicsOAuthTokenClient> oauthTokenClient,
            @Value("${app.diagnostics.dir:diagnostics}") String diagnosticsDir
    ) {
        this.dynamicsClient = dynamicsClient;
        this.properties = properties;
        this.oauthTokenClient = oauthTokenClient;
        this.diagnosticsDir = Path.of(diagnosticsDir).toAbsolutePath().normalize();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Captura el estado actual del lote y lo guarda como {@code {lote}_{fase}.json}.
     *
     * @param fase p.ej. ANTES o DESPUES
     */
    public LiberacionDiagnosticoDtos.Captura capturarYGuardar(String rawLote, String fase) {
        String lote = requireLote(rawLote);
        String faseNorm = requireFase(fase);
        String token = requestToken();

        Optional<DynamicsClient.ItemBatchRecord> batchOpt =
                dynamicsClient.findItemBatch(lote, token);
        if (batchOpt.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND,
                    "Lote no encontrado en ItemBatches: " + lote);
        }
        DynamicsClient.ItemBatchRecord batch = batchOpt.get();

        Optional<DynamicsClient.QualityOrderRecord> qualityOpt =
                dynamicsClient.findQualityOrderByItemBatch(lote, token);

        LiberacionDiagnosticoDtos.ItemBatchesSnap itemSnap = new LiberacionDiagnosticoDtos.ItemBatchesSnap(
                batch.batchNumber() != null ? batch.batchNumber() : lote,
                blankToNull(batch.batchDispositionCode()),
                blankToNull(batch.batchExpirationDate())
        );

        LiberacionDiagnosticoDtos.QualityOrderSnap qualitySnap;
        boolean qualityFound;
        if (qualityOpt.isPresent()) {
            DynamicsClient.QualityOrderRecord q = qualityOpt.get();
            qualitySnap = new LiberacionDiagnosticoDtos.QualityOrderSnap(
                    blankToNull(q.qualityOrderStatus()),
                    blankToNull(q.passedBatchDispositionCode())
            );
            qualityFound = true;
        } else {
            qualitySnap = new LiberacionDiagnosticoDtos.QualityOrderSnap(null, null);
            qualityFound = false;
        }

        Path archivo = diagnosticsDir.resolve(lote + "_" + faseNorm + ".json");
        LiberacionDiagnosticoDtos.Captura captura = new LiberacionDiagnosticoDtos.Captura(
                lote,
                faseNorm,
                Instant.now(),
                archivo.toString(),
                itemSnap,
                qualitySnap,
                true,
                qualityFound
        );

        guardarJson(archivo, captura);
        logCaptura(captura);
        return captura;
    }

    private void guardarJson(Path archivo, LiberacionDiagnosticoDtos.Captura captura) {
        try {
            Files.createDirectories(archivo.getParent());
            mapper.writeValue(archivo.toFile(), captura);
            log.info("[DiagnosticoLiberacion] archivo guardado={}", archivo);
        } catch (Exception ex) {
            log.warn("[DiagnosticoLiberacion] no se pudo guardar archivo={} tipo={}",
                    archivo, ex.getClass().getSimpleName());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "No se pudo guardar evidencia en " + archivo + ": " + ex.getMessage());
        }
    }

    private void logCaptura(LiberacionDiagnosticoDtos.Captura c) {
        log.info("[DiagnosticoLiberacion] fase={} lote={} BatchDispositionCode={} QualityOrderStatus={} PassedBatchDispositionCode={} BatchExpirationDate={} archivo={}",
                c.fase(),
                c.lote(),
                dash(c.itemBatches().batchDispositionCode()),
                dash(c.qualityOrderHeaders().qualityOrderStatus()),
                dash(c.qualityOrderHeaders().passedBatchDispositionCode()),
                dash(c.itemBatches().batchExpirationDate()),
                c.archivo());
    }

    private String requireLote(String raw) {
        return LoteExtractor.extract(raw)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Lote requerido"));
    }

    private static String requireFase(String fase) {
        if (fase == null || fase.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "fase requerida (ANTES o DESPUES)");
        }
        String n = fase.trim().toUpperCase(Locale.ROOT);
        if (!"ANTES".equals(n) && !"DESPUES".equals(n)) {
            throw new ResponseStatusException(BAD_REQUEST, "fase debe ser ANTES o DESPUES");
        }
        return n;
    }

    private String requestToken() {
        DynamicsOAuthTokenClient oauth = oauthTokenClient.getIfAvailable();
        if (oauth != null) {
            return oauth.requestAccessToken();
        }
        log.debug("[DiagnosticoLiberacion] modo={} sin OAuth (mock)", properties.getMode());
        return "MOCK_TOKEN";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
