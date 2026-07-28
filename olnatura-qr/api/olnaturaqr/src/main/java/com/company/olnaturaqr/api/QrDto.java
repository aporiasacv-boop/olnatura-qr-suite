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
            /** Workflow interno (approve/reject). No modifica Estado Operativo Dynamics. */
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
            /** Corrección admin de {@code qr_labels.status} (platformStatus). Nunca Estado Operativo. */
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
             * Solo lectura: lo calcula {@code OperationalStatusResolver} desde Dynamics.
             * La aplicación <strong>nunca</strong> escribe este valor; no usar {@code qr_labels.status}.
             */
            String status,
            /** Regla aplicada al Estado Operativo (transparencia). */
            String operationalStatusRule,
            /** Fuente textual del Estado Operativo (Dynamics). */
            String statusSource,
            /**
             * Estado de plataforma ({@code qr_labels.status}): workflow de aprobaciones
             * y corrección administrativa. Independiente del banner / Estado Operativo.
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
            String fuente,
            /**
             * Momento en que se completó la lectura OData de Dynamics para esta respuesta.
             * No se persiste; solo refleja esta consulta. Null solo si no hubo intento de lookup.
             * La acción «Sincronizar con Dynamics» fuerza una nueva lectura y actualiza este valor.
             */
            Instant lastSyncedAt
    ) {}

    public record Response(
            Label label,
            Dynamic dynamic,
            List<String> availableTransitions,
            Permissions permissions
    ) {}
}
