package com.company.olnaturaqr.support.diagnostics;

import java.time.Instant;

/**
 * Evidencia Dynamics para diagnóstico de liberación.
 * Solo captura; sin comparación ni sincronización.
 */
public final class LiberacionDiagnosticoDtos {

    private LiberacionDiagnosticoDtos() {}

    public record ItemBatchesSnap(
            String batchNumber,
            String batchDispositionCode,
            String batchExpirationDate
    ) {}

    public record QualityOrderSnap(
            String qualityOrderStatus,
            String passedBatchDispositionCode
    ) {}

    public record Captura(
            String lote,
            String fase,
            Instant capturadoEn,
            String archivo,
            ItemBatchesSnap itemBatches,
            QualityOrderSnap qualityOrderHeaders,
            boolean itemBatchesEncontrado,
            boolean qualityOrderEncontrado
    ) {}
}
