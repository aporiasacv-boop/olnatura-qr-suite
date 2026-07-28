package com.company.olnaturaqr.infra.dynamics;

/**
 * Resultado orquestado de consulta Dynamics por BatchNumber, listo para el frontend.
 */
public record DynamicsLookupDto(
        String codigo,
        String nombre,
        String lote,
        String caducidad,
        Double cantidadAlmacen,
        /** InventoryUnitSymbol desde ReleasedProductsV2; null si no hay unidad. */
        String unidadInventario,
        /**
         * Fecha de entrada del lote desde InventTrans (MIN DatePhysical Received|Purchased).
         * ISO Dynamics, p.ej. 2026-07-21T12:00:00Z; null si no se pudo resolver.
         */
        String fechaEntrada,
        /**
         * Estado Operativo (APROBADO|CUARENTENA|RECHAZADO|DESCONOCIDO).
         * Solo lectura: {@link com.company.olnaturaqr.support.workflow.OperationalStatusResolver}.
         * No usa ni escribe {@code qr_labels.status}.
         */
        String operationalStatus,
        /** Regla aplicada: "Almacén REM" | "Almacén RES" | "Almacén CUARENTENA" | "BatchDispositionCode" | … */
        String operationalStatusRule,
        /** Texto fijo de transparencia: Dynamics 365 Finance & Operations */
        String statusSource,
        /**
         * Compat / resumen: QualityOrderStatus (informativo; no sincroniza estado QR).
         * Preferir qualityOrderStatus / passedBatchDispositionCode / batchDispositionCode.
         */
        String statusDynamics,
        /** QualityOrderHeaders.QualityOrderStatus — solo diagnóstico/referencia. */
        String qualityOrderStatus,
        /** QualityOrderHeaders.PassedBatchDispositionCode — solo diagnóstico/referencia. */
        String passedBatchDispositionCode,
        /** ItemBatches.BatchDispositionCode — solo diagnóstico/referencia. */
        String batchDispositionCode,
        String almacen,
        String ubicacion,
        String fuente
) {}
