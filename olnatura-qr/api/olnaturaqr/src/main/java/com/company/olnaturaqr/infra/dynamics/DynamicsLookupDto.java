package com.company.olnaturaqr.infra.dynamics;

import java.util.List;


public record DynamicsLookupDto(
        String codigo,
        String nombre,
        String lote,
        String caducidad,
        Double cantidadAlmacen,
        Double cantidadRecibida,
        
        String unidadInventario,
        
        String fechaEntrada,
        
        String operationalStatus,
        
        String operationalStatusRule,
        
        String statusSource,
        
        String statusDynamics,
        
        String qualityOrderStatus,
        
        String passedBatchDispositionCode,
        
        String batchDispositionCode,
        String almacen,
        String ubicacion,
        String fuente,
        
        String fechaLiberacion,
        
        String liberadoPor,

        /** Almacenes donde el lote tiene existencia distinta de cero (ProjInventoryOnHand). Sin consultas extras. */
        List<String> warehouses,

        /** Existencia del lote en almacén de uso (MEM/MES/MPS/MPM, fuera de la ubicación Rechazo); null si es cero. */
        Double cantidadAprobada,

        /** Existencia del lote en REM/RES/REM-D/RES-D o en ubicación Rechazo; null si es cero. */
        Double cantidadRechazada,

        /** Almacenes (y ubicación Rechazo) donde está la parte rechazada, p. ej. "REM" o "MPM/Rechazo". */
        List<String> almacenesRechazo
) {}
