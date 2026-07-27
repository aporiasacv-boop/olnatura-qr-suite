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
