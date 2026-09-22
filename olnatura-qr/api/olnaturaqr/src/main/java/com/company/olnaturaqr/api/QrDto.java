package com.company.olnaturaqr.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class QrDto {

    public record ApprovalLeg(
            boolean approved,
            String actorEmail,
            Instant at,
            String rol
    ) {}

    public record Permissions(
            
            boolean canChangeStatus,
            boolean canRegisterScan,
            boolean canCreateLabel,
            boolean canApproveCalidad,
            boolean canApproveInspeccion,
            boolean canReject,
            boolean canDownloadAuditPdf,
            boolean calidadApproved,
            boolean inspeccionApproved,
            String pendingMessage,
            String tipoMaterialDisplay,
            ApprovalLeg calidad,
            ApprovalLeg inspeccion,
            boolean canCorrectLabel,
            
            boolean canCorrectStatus,
            List<String> allowedStatusCorrections
    ) {}

    public record Label(
            String tipoMaterial,
            String nombre,
            String codigo,
            String lote,
            String publicToken,
            LocalDate fechaEntrada,
            LocalDate caducidad,
            LocalDate reanalisis,
            int envaseNum,
            int envaseTotal,
            String cantidadPorEnvase,
            boolean restosEnabled,
            String cantidadResto,
            List<String> restosCantidades,
            boolean reprintRequired,
            String id
    ) {}

    public record Dynamic(
            String codigo,
            String nombre,
            String lote,
            String caducidad,
            Double cantidadAlmacen,
            Double cantidadRecibida,
            
            String unidadInventario,
            
            String fechaEntrada,
            
            String status,
            
            String operationalStatusRule,
            
            String statusSource,
            
            String platformStatus,
            
            String statusDynamics,
            
            String qualityOrderStatus,
            
            String passedBatchDispositionCode,
            
            String batchDispositionCode,
            String almacen,
            String ubicacion,
            String fuente,
            
            Instant lastSyncedAt,
            
            String fechaLiberacion,
            
            String liberadoPor
    ) {}

    public record Response(
            Label label,
            Dynamic dynamic,
            List<String> availableTransitions,
            Permissions permissions
    ) {}
}
