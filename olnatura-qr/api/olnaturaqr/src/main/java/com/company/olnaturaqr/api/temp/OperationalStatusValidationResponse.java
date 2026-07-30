package com.company.olnaturaqr.api.temp;

import java.util.List;

/**
 * TEMPORAL — validación funcional Estado Operativo (eliminar con el paquete {@code api.temp}).
 * El estado proviene del {@code OperationalStatusResolver} vía {@code DynamicsLookupService};
 * no se aplica {@code OperationalStatusPresentation.forUi}.
 */
public record OperationalStatusValidationResponse(
        String lote,
        String codigo,
        String nombre,
        String caducidad,
        Double cantidadAlmacen,
        String unidadInventario,
        String fechaEntrada,
        String almacen,
        String ubicacion,
        String fuente,
        String statusDynamics,
        String qualityOrderStatus,
        String passedBatchDispositionCode,
        String batchDispositionCode,
        List<String> warehouses,
        String fechaLiberacion,
        String liberadoPor,
        /** Resultado crudo del resolver (APROBADO / CUARENTENA / RECHAZADO / DESCONOCIDO). */
        String operationalStatus,
        String operationalStatusRule,
        String statusSource,
        List<ReasonLine> reasons
) {
    public record ReasonLine(boolean matched, String text) {}
}
