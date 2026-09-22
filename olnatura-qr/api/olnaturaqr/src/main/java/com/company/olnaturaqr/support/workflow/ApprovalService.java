package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class ApprovalService {

    private static final String STATUS_ONLY_FROM_DYNAMICS =
            "El estado operativo del lote solo puede cambiarse desde Dynamics";

    private final AuditEventRepository auditEventRepository;

    public ApprovalService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public ApprovalView view(QrLabel label, AuthPrincipal principal) {
        String tipo = MaterialType.normalize(label.getTipoMaterial());
        String status = WorkflowStatus.normalize(label.getStatus());
        ApprovalTrace.Snapshot snap = loadSnapshot(label);

        return new ApprovalView(
                status,
                MaterialType.display(tipo),
                tipo,
                snap.calidadDone(),
                snap.inspeccionDone(),
                false,
                false,
                false,
                false,
                null,
                toLegView(snap.calidad()),
                toLegView(snap.inspeccion())
        );
    }

    @Transactional
    public QrLabel approve(QrLabel label, AuthPrincipal principal) {
        return approve(label, principal, null);
    }

    @Transactional
    public QrLabel approve(QrLabel label, AuthPrincipal principal, String motivo) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, STATUS_ONLY_FROM_DYNAMICS);
    }

    @Transactional
    public QrLabel reject(QrLabel label, AuthPrincipal principal) {
        return reject(label, principal, null);
    }

    @Transactional
    public QrLabel reject(QrLabel label, AuthPrincipal principal, String motivo) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, STATUS_ONLY_FROM_DYNAMICS);
    }

    private ApprovalTrace.Snapshot loadSnapshot(QrLabel label) {
        ApprovalTrace.Snapshot fromAudit = ApprovalTrace.fromAudit(auditEventRepository, label.getLote());
        return ApprovalTrace.withLegacyFallback(fromAudit, label);
    }

    private static ApprovalLegView toLegView(ApprovalTrace.Leg leg) {
        if (leg == null || !leg.approved()) {
            return new ApprovalLegView(false, null, null, null);
        }
        return new ApprovalLegView(true, leg.actorEmail(), leg.at(), leg.rol());
    }

    public record ApprovalLegView(
            boolean approved,
            String actorEmail,
            Instant at,
            String rol
    ) {}

    public record ApprovalView(
            String status,
            String tipoMaterialDisplay,
            String tipoMaterial,
            boolean calidadApproved,
            boolean inspeccionApproved,
            boolean canApproveCalidad,
            boolean canApproveInspeccion,
            boolean canReject,
            boolean canChangeStatus,
            String pendingMessage,
            ApprovalLegView calidad,
            ApprovalLegView inspeccion
    ) {}
}
