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
        String statusDynamics,
        String almacen,
        String ubicacion,
        String fuente
) {}
