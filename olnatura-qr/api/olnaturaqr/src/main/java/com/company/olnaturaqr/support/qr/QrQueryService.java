package com.company.olnaturaqr.support.qr;

import com.company.olnaturaqr.api.QrDto;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.ApprovalService;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.support.workflow.WorkflowTransitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public QrDto.Response getByLote(String loteRaw, AuthPrincipal principal) {
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

        // Estado del material: solo plataforma (CUARENTENA/APROBADO/RECHAZADO). Nunca Dynamics Open/Pending.
        String platformStatus = WorkflowStatus.normalize(label.getStatus());

        QrDto.Dynamic dyn = lookupDynamicsOrFail(lote)
                .map(d -> {
                    logEstadoDiag(lote, platformStatus, d);
                    return toDynamicDto(d, platformStatus);
                })
                .orElseGet(() -> {
                    log.info("[EstadoDiag] lote={} EstadoQR={} QualityOrderStatus=— PassedBatchDispositionCode=— BatchDispositionCode=— (sin Dynamics)",
                            lote, platformStatus);
                    return new QrDto.Dynamic(
                        label.getCodigo(),
                        label.getNombre(),
                        lote,
                        label.getCaducidad() != null ? label.getCaducidad().toString() : null,
                        null,
                        null,
                        null,
                        platformStatus,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DB_ONLY"
                    );
                });

        List<String> transitions = principal != null
                ? WorkflowTransitions.allowedFrom(platformStatus)
                : Collections.emptyList();

        ApprovalService.ApprovalView av = approvalService.view(label, principal);
        QrDto.Permissions perms = buildPermissions(principal, av);

        return new QrDto.Response(dtoLabel, dyn, transitions, perms);
    }

    private Optional<DynamicsLookupDto> lookupDynamicsOrFail(String lote) {
        return dynamicsLookupService.lookupByBatchNumber(lote);
    }

    private static QrDto.Dynamic toDynamicDto(DynamicsLookupDto d, String platformStatus) {
        return new QrDto.Dynamic(
                d.codigo(),
                d.nombre(),
                d.lote(),
                d.caducidad(),
                d.cantidadAlmacen(),
                d.unidadInventario(),
                d.fechaEntrada(),
                platformStatus,
                d.statusDynamics(),
                d.qualityOrderStatus(),
                d.passedBatchDispositionCode(),
                d.batchDispositionCode(),
                d.almacen(),
                d.ubicacion(),
                d.fuente()
        );
    }

    /** Log temporal: comparar estado QR (fuente de verdad) vs campos Dynamics (solo referencia). */
    private static void logEstadoDiag(String lote, String estadoQr, DynamicsLookupDto d) {
        log.info("[EstadoDiag] lote={} EstadoQR={} QualityOrderStatus={} PassedBatchDispositionCode={} BatchDispositionCode={}",
                lote,
                estadoQr,
                dash(d.qualityOrderStatus()),
                dash(d.passedBatchDispositionCode()),
                dash(d.batchDispositionCode()));
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private QrDto.Permissions buildPermissions(AuthPrincipal principal, ApprovalService.ApprovalView av) {
        QrDto.ApprovalLeg calidad = toLeg(av != null ? av.calidad() : null);
        QrDto.ApprovalLeg inspeccion = toLeg(av != null ? av.inspeccion() : null);
        if (principal == null || principal.roles() == null) {
            return new QrDto.Permissions(
                    false, false, false, false, false, false, false,
                    false, false, null, av != null ? av.tipoMaterialDisplay() : null,
                    calidad, inspeccion
            );
        }
        var roles = principal.roles();
        boolean canCreateLabel = rolesContains(roles, "ADMIN")
                || rolesContains(roles, "ALMACEN")
                || rolesContains(roles, "PRODUCCION")
                || rolesContains(roles, "CALIDAD")
                || rolesContains(roles, "INSPECCION");
        // Registrar etiqueta (POST) sigue restringido a ADMIN/ALMACEN en el controller.
        boolean canRegisterScan = rolesContains(roles, "ADMIN")
                || rolesContains(roles, "ALMACEN")
                || rolesContains(roles, "PRODUCCION")
                || rolesContains(roles, "CALIDAD")
                || rolesContains(roles, "INSPECCION");
        boolean canDownloadAuditPdf = rolesContains(roles, "ADMIN")
                || rolesContains(roles, "CALIDAD")
                || rolesContains(roles, "INSPECCION");

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
                inspeccion
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
