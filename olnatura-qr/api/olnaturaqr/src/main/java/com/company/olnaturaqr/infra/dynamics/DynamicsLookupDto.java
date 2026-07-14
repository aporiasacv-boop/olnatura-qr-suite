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
        String statusDynamics,
        String almacen,
        String ubicacion,
        String fuente
) {}
