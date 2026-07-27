package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ApprovalService {

    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;

    public ApprovalService(
            QrLabelRepository qrLabelRepository,
            AuditService auditService,
            AuditEventRepository auditEventRepository
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
        this.auditEventRepository = auditEventRepository;
    }

    public ApprovalView view(QrLabel label, AuthPrincipal principal) {
        String tipo = MaterialType.normalize(label.getTipoMaterial());
        String status = WorkflowStatus.normalize(label.getStatus());
        ApprovalTrace.Snapshot snap = loadSnapshot(label);

        boolean calidadDone = snap.calidadDone();
        boolean inspeccionDone = snap.inspeccionDone();

        boolean needsCalidad = MaterialType.requiresCalidad(tipo);
        boolean needsInspeccion = MaterialType.requiresInspeccion(tipo);

        boolean isAdmin = hasRole(principal, "ADMIN");
        boolean isCalidad = hasRole(principal, "CALIDAD") || isAdmin;
        boolean isInspeccion = hasRole(principal, "INSPECCION") || isAdmin;

        boolean canApproveCalidad = WorkflowStatus.CUARENTENA.equals(status)
                && needsCalidad && isCalidad && !calidadDone;
        boolean canApproveInspeccion = WorkflowStatus.CUARENTENA.equals(status)
                && needsInspeccion && isInspeccion && !inspeccionDone;
        boolean canReject = WorkflowStatus.CUARENTENA.equals(status)
                && (isCalidad || isInspeccion)
                && ((needsCalidad && isCalidad) || (needsInspeccion && isInspeccion) || isAdmin);

        String pendingMessage = null;
        if (WorkflowStatus.CUARENTENA.equals(status) && MaterialType.EMPAQUE_PRIMARIO.equals(tipo)) {
            if (!calidadDone && inspeccionDone) {
                pendingMessage = "Falta aprobación de Calidad";
            } else if (calidadDone && !inspeccionDone) {
                pendingMessage = "Falta aprobación de Inspección";
            } else if (!calidadDone && !inspeccionDone) {
                pendingMessage = "Falta aprobación de Calidad e Inspección";
            }
        }

        boolean canChangeStatus = canApproveCalidad || canApproveInspeccion || canReject;

        return new ApprovalView(
                status,
                MaterialType.display(tipo),
                tipo,
                calidadDone,
                inspeccionDone,
                canApproveCalidad,
                canApproveInspeccion,
                canReject,
                canChangeStatus,
                pendingMessage,
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
        LotOperationalGate.requireActive(label);
        String status = WorkflowStatus.normalize(label.getStatus());
        if (!WorkflowStatus.CUARENTENA.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estado del lote es definitivo y no puede modificarse");
        }

        String tipo = MaterialType.normalize(label.getTipoMaterial());
        if (!MaterialType.isValid(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de material inválido para aprobación: " + label.getTipoMaterial());
        }

        ApprovalTrace.Snapshot snap = loadSnapshot(label);
        boolean calidadDone = snap.calidadDone();
        boolean inspeccionDone = snap.inspeccionDone();

        boolean isAdmin = hasRole(principal, "ADMIN");
        boolean isCalidad = hasRole(principal, "CALIDAD") || isAdmin;
        boolean isInspeccion = hasRole(principal, "INSPECCION") || isAdmin;

        String approvalRole;
        if (MaterialType.MATERIA_PRIMA.equals(tipo)) {
            if (!isCalidad) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo Control de Calidad puede aprobar Materia Prima");
            }
            if (calidadDone) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Calidad ya aprobó este lote");
            }
            label.setStatus(WorkflowStatus.APROBADO);
            approvalRole = "CALIDAD";
            calidadDone = true;
        } else if (MaterialType.EMPAQUE_SECUNDARIO.equals(tipo)) {
            if (!isInspeccion) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo Inspección puede aprobar Empaque Secundario");
            }
            if (inspeccionDone) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Inspección ya aprobó este lote");
            }
            label.setStatus(WorkflowStatus.APROBADO);
            approvalRole = "INSPECCION";
            inspeccionDone = true;
        } else if (MaterialType.EMPAQUE_PRIMARIO.equals(tipo)) {
            boolean needCalidad = !calidadDone;
            boolean needInspeccion = !inspeccionDone;
            if (!needCalidad && !needInspeccion) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existen ambas aprobaciones");
            }
            if (needCalidad && isCalidad) {
                approvalRole = "CALIDAD";
                calidadDone = true;
            } else if (needInspeccion && isInspeccion) {
                approvalRole = "INSPECCION";
                inspeccionDone = true;
            } else if (needCalidad) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Falta aprobación de Calidad");
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Falta aprobación de Inspección");
            }
            if (calidadDone && inspeccionDone) {
                label.setStatus(WorkflowStatus.APROBADO);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de material no soportado");
        }

        // Trazabilidad de quién/cuándo: solo auditoría (no columnas en qr_labels).
        QrLabel saved = qrLabelRepository.save(label);
        String resulting = WorkflowStatus.normalize(saved.getStatus());
        auditService.log(
                principal,
                "APPROVE_MATERIAL",
                saved.getLote(),
                ApprovalTrace.approveMetadata(
                        saved, tipo, approvalRole, resulting, calidadDone, inspeccionDone, motivo),
                null
        );
        return saved;
    }

    @Transactional
    public QrLabel reject(QrLabel label, AuthPrincipal principal) {
        return reject(label, principal, null);
    }

    @Transactional
    public QrLabel reject(QrLabel label, AuthPrincipal principal, String motivo) {
        LotOperationalGate.requireActive(label);
        String status = WorkflowStatus.normalize(label.getStatus());
        if (!WorkflowStatus.CUARENTENA.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estado del lote es definitivo y no puede modificarse");
        }

        String tipo = MaterialType.normalize(label.getTipoMaterial());
        boolean isAdmin = hasRole(principal, "ADMIN");
        boolean isCalidad = hasRole(principal, "CALIDAD") || isAdmin;
        boolean isInspeccion = hasRole(principal, "INSPECCION") || isAdmin;

        boolean allowed =
                (MaterialType.requiresCalidad(tipo) && isCalidad)
                        || (MaterialType.requiresInspeccion(tipo) && isInspeccion)
                        || isAdmin;
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para rechazar este material");
        }

        String rejectRole = isCalidad && hasRole(principal, "CALIDAD")
                ? "CALIDAD"
                : (isInspeccion && hasRole(principal, "INSPECCION") ? "INSPECCION" : (isAdmin ? "ADMIN" : null));

        label.setStatus(WorkflowStatus.RECHAZADO);
        QrLabel saved = qrLabelRepository.save(label);

        Map<String, Object> md = new LinkedHashMap<>(ApprovalTrace.rejectMetadata(saved, tipo, motivo));
        if (rejectRole != null) {
            md.put("rol", rejectRole);
        }
        auditService.log(principal, "REJECT_MATERIAL", saved.getLote(), md, null);
        return saved;
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

    private static boolean hasRole(AuthPrincipal principal, String role) {
        if (principal == null || principal.roles() == null) return false;
        String target = role.toUpperCase(Locale.ROOT);
        return principal.roles().stream().anyMatch(r -> target.equalsIgnoreCase(r));
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
