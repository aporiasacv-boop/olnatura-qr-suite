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
            String cantidadPorEnvase
    ) {}

    public record Dynamic(
            String codigo,
            String nombre,
            String lote,
            String caducidad,
            Double cantidadAlmacen,
            /** InventoryUnitSymbol (ReleasedProductsV2); null si no aplica. */
            String unidadInventario,
            /** Fecha de entrada Dynamics (MIN DatePhysical Received|Purchased); null si no aplica. */
            String fechaEntrada,
            /**
             * Estado Operativo (banner): APROBADO|CUARENTENA|RECHAZADO|DESCONOCIDO.
             * Interpretado desde Dynamics; no usa qr_labels.status.
             */
            String status,
            /** Regla aplicada al Estado Operativo (transparencia). */
            String operationalStatusRule,
            /** Fuente textual del Estado Operativo. */
            String statusSource,
            /**
             * Estado interno de plataforma ({@code qr_labels.status}), solo compatibilidad /
             * workflow de aprobaciones y corrección admin. No es el banner.
             */
            String platformStatus,
            /** Resumen informativo Dynamics (QualityOrderStatus); no sincroniza estado QR. */
            String statusDynamics,
            /** QualityOrderHeaders.QualityOrderStatus — diagnóstico. */
            String qualityOrderStatus,
            /** QualityOrderHeaders.PassedBatchDispositionCode — diagnóstico. */
            String passedBatchDispositionCode,
            /** ItemBatches.BatchDispositionCode — diagnóstico. */
            String batchDispositionCode,
            String almacen,
            String ubicacion,
            String fuente
    ) {}

    public record Response(
            Label label,
            Dynamic dynamic,
            List<String> availableTransitions,
            Permissions permissions
    ) {}
}
