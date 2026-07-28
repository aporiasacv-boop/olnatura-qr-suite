package com.company.olnaturaqr.support.qr;

import com.company.olnaturaqr.api.QrDto;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.AdminStatusCorrectionService;
import com.company.olnaturaqr.support.workflow.ApprovalService;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
import com.company.olnaturaqr.support.workflow.OperationalStatusResolver;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.support.workflow.WorkflowTransitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class QrQueryService {

    private static final Logger log = LoggerFactory.getLogger(QrQueryService.class);

    private final QrLabelRepository qrLabelRepository;
    private final DynamicsLookupService dynamicsLookupService;
    private final ApprovalService approvalService;

    public QrQueryService(
            QrLabelRepository qrLabelRepository,
            DynamicsLookupService dynamicsLookupService,
            ApprovalService approvalService
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.dynamicsLookupService = dynamicsLookupService;
        this.approvalService = approvalService;
    }

    /**
     * Consulta por lote: lee etiqueta en BD y vuelve a consultar Dynamics en vivo.
     * No escribe en Dynamics ni muta Estado Operativo / platformStatus.
     */
    @Transactional(readOnly = true)
    public QrDto.Response getByLote(String loteRaw, AuthPrincipal principal) {
        return buildResponse(loteRaw, principal, false);
    }

    /**
     * Sincronización manual: fuerza una nueva lectura OData del ERP para el lote.
     * <p><strong>Solo lectura.</strong> No modifica Dynamics, no cambia estados,
     * no ejecuta aprobaciones ni correcciones administrativas, no persiste
     * información operacional. Reutiliza el mismo ensamblaje que {@link #getByLote}.
     */
    @Transactional(readOnly = true)
    public QrDto.Response syncWithDynamics(String loteRaw, AuthPrincipal principal) {
        return buildResponse(loteRaw, principal, true);
    }

    private QrDto.Response buildResponse(String loteRaw, AuthPrincipal principal, boolean manualSync) {
        String identifier = LoteExtractor.extract(loteRaw)
                .orElse(loteRaw != null ? loteRaw.trim() : "");

        if (identifier.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Identificador requerido");
        }
        if (identifier.length() > 120) {
            throw new ResponseStatusException(BAD_REQUEST, "Identificador demasiado largo");
        }

        QrLabel label = qrLabelRepository.findByPublicToken(identifier)
                .or(() -> qrLabelRepository.findByLote(identifier))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + identifier));

        LotOperationalGate.requireActive(label);

        String lote = label.getLote();
        if (manualSync) {
            log.info("[SyncDynamics] Lectura manual solicitada lote={} (solo consulta OData; sin escritura en ERP)", lote);
        }

        var dtoLabel = new QrDto.Label(
                label.getTipoMaterial(),
                label.getNombre(),
                label.getCodigo(),
                label.getLote(),
                label.getPublicToken(),
                label.getFechaEntrada(),
                label.getCaducidad(),
                label.getReanalisis(),
                label.getEnvaseNum(),
                label.getEnvaseTotal(),
                label.getCantidadPorEnvase()
        );

        // platformStatus = qr_labels.status (workflow interno). Independiente del Estado Operativo.
        String platformStatus = WorkflowStatus.normalize(label.getStatus());
        Instant syncedAt = Instant.now();

        QrDto.Dynamic dyn = lookupDynamicsOrFail(lote)
                .map(d -> {
                    logEstadoDiag(lote, platformStatus, d);
                    return toDynamicDto(d, platformStatus, syncedAt);
                })
                .orElseGet(() -> {
                    log.info("[EstadoOperativo] lote={} status=DESCONOCIDO rule=Información insuficiente (sin Dynamics) platformStatus={}",
                            lote, platformStatus);
                    return new QrDto.Dynamic(
                        label.getCodigo(),
                        label.getNombre(),
                        lote,
                        label.getCaducidad() != null ? label.getCaducidad().toString() : null,
                        null,
                        null,
                        null,
                        OperationalStatusResolver.STATUS_DESCONOCIDO,
                        OperationalStatusResolver.RULE_INSUFFICIENT,
                        OperationalStatusResolver.SOURCE_DYNAMICS,
                        platformStatus,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DB_ONLY",
                        syncedAt
                    );
                });

        List<String> transitions = principal != null
                ? WorkflowTransitions.allowedFrom(platformStatus)
                : Collections.emptyList();

        ApprovalService.ApprovalView av = approvalService.view(label, principal);
        QrDto.Permissions perms = buildPermissions(principal, av, platformStatus);

        return new QrDto.Response(dtoLabel, dyn, transitions, perms);
    }

    private Optional<DynamicsLookupDto> lookupDynamicsOrFail(String lote) {
        return dynamicsLookupService.lookupByBatchNumber(lote);
    }

    private static QrDto.Dynamic toDynamicDto(
            DynamicsLookupDto d,
            String platformStatus,
            Instant lastSyncedAt
    ) {
        return new QrDto.Dynamic(
                d.codigo(),
                d.nombre(),
                d.lote(),
                d.caducidad(),
                d.cantidadAlmacen(),
                d.unidadInventario(),
                d.fechaEntrada(),
                d.operationalStatus(),
                d.operationalStatusRule(),
                d.statusSource(),
                platformStatus,
                d.statusDynamics(),
                d.qualityOrderStatus(),
                d.passedBatchDispositionCode(),
                d.batchDispositionCode(),
                d.almacen(),
                d.ubicacion(),
                d.fuente(),
                lastSyncedAt
        );
    }

    private static void logEstadoDiag(String lote, String platformStatus, DynamicsLookupDto d) {
        log.info("[EstadoOperativo] lote={} operationalStatus={} rule={} platformStatus(histórico)={} BatchDispositionCode={}",
                lote,
                d.operationalStatus(),
                d.operationalStatusRule(),
                platformStatus,
                dash(d.batchDispositionCode()));
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private QrDto.Permissions buildPermissions(
            AuthPrincipal principal,
            ApprovalService.ApprovalView av,
            String platformStatus
    ) {
        QrDto.ApprovalLeg calidad = toLeg(av != null ? av.calidad() : null);
        QrDto.ApprovalLeg inspeccion = toLeg(av != null ? av.inspeccion() : null);
        List<String> adminStatusTargets = AdminStatusCorrectionService.allowedTargets(platformStatus);
        if (principal == null || principal.roles() == null) {
            return new QrDto.Permissions(
                    false, false, false, false, false, false, false,
                    false, false, null, av != null ? av.tipoMaterialDisplay() : null,
                    calidad, inspeccion, false, false, Collections.emptyList()
            );
        }
        var roles = principal.roles();
        boolean isAdmin = rolesContains(roles, "ADMIN");
        boolean canCreateLabel = isAdmin
                || rolesContains(roles, "ALMACEN")
                || rolesContains(roles, "PRODUCCION")
                || rolesContains(roles, "CALIDAD")
                || rolesContains(roles, "INSPECCION");
        boolean canRegisterScan = isAdmin
                || rolesContains(roles, "ALMACEN")
                || rolesContains(roles, "PRODUCCION")
                || rolesContains(roles, "CALIDAD")
                || rolesContains(roles, "INSPECCION");
        boolean canDownloadAuditPdf = isAdmin
                || rolesContains(roles, "CALIDAD")
                || rolesContains(roles, "INSPECCION");
        boolean canCorrectStatus = isAdmin && !adminStatusTargets.isEmpty();

        return new QrDto.Permissions(
                av.canChangeStatus(),
                canRegisterScan,
                canCreateLabel,
                av.canApproveCalidad(),
                av.canApproveInspeccion(),
                av.canReject(),
                canDownloadAuditPdf,
                av.calidadApproved(),
                av.inspeccionApproved(),
                av.pendingMessage(),
                av.tipoMaterialDisplay(),
                calidad,
                inspeccion,
                isAdmin,
                canCorrectStatus,
                canCorrectStatus ? adminStatusTargets : Collections.emptyList()
        );
    }

    private static QrDto.ApprovalLeg toLeg(ApprovalService.ApprovalLegView leg) {
        if (leg == null) {
            return new QrDto.ApprovalLeg(false, null, null, null);
        }
        return new QrDto.ApprovalLeg(leg.approved(), leg.actorEmail(), leg.at(), leg.rol());
    }

    private static boolean rolesContains(List<String> roles, String role) {
        String t = role.toUpperCase(Locale.ROOT);
        return roles.stream().anyMatch(r -> t.equalsIgnoreCase(r));
    }
}
