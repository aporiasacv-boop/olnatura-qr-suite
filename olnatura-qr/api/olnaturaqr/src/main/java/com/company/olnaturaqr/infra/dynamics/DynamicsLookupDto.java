package com.company.olnaturaqr.infra.dynamics;

import java.util.List;


public record DynamicsLookupDto(
        String codigo,
        String nombre,
        String lote,
        String caducidad,
        Double cantidadAlmacen,
        
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

        /** Almacenes ya resueltos en el lookup (InventDim + QualityOrder). Sin consultas extras. */
        List<String> warehouses
) {}
