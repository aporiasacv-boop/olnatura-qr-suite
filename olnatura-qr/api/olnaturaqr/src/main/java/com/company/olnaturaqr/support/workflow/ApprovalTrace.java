package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.AuditEventRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reconstruye el estado de aprobación parcial desde auditoría
 * (APPROVE_MATERIAL), sin depender de columnas en qr_labels.
 */
final class ApprovalTrace {

    private ApprovalTrace() {}

    record Leg(boolean approved, String actorEmail, Instant at, String rol) {
        static Leg empty() {
            return new Leg(false, null, null, null);
        }
    }

    record Snapshot(Leg calidad, Leg inspeccion) {
        boolean calidadDone() {
            return calidad != null && calidad.approved();
        }

        boolean inspeccionDone() {
            return inspeccion != null && inspeccion.approved();
        }
    }

    static Snapshot fromAudit(AuditEventRepository repo, String lote) {
        if (lote == null || lote.isBlank()) {
            return new Snapshot(Leg.empty(), Leg.empty());
        }
        List<AuditEvent> events = repo.findByLoteAndActionTypeInOrderByCreatedAtAsc(
                lote.trim(),
                List.of("APPROVE_MATERIAL", "REJECT_MATERIAL")
        );

        Leg calidad = Leg.empty();
        Leg inspeccion = Leg.empty();

        for (AuditEvent e : events) {
            String action = e.getActionType() == null ? "" : e.getActionType().trim().toUpperCase(Locale.ROOT);
            if ("REJECT_MATERIAL".equals(action)) {
                // El rechazo no borra el historial de quién aprobó; solo cierra el lote.
                continue;
            }
            if (!"APPROVE_MATERIAL".equals(action)) {
                continue;
            }
            String role = effectiveRole(e);
            String email = e.getActorEmail();
            Instant at = e.getCreatedAt();
            if ("CALIDAD".equals(role)) {
                calidad = new Leg(true, email, at, "CALIDAD");
            } else if ("INSPECCION".equals(role)) {
                inspeccion = new Leg(true, email, at, "INSPECCION");
            }
        }
        return new Snapshot(calidad, inspeccion);
    }

    /**
     * Compatibilidad: si aún hay marcas antiguas en la etiqueta y no hay eventos,
     * úsalas solo como fallback de lectura (sin escribir de nuevo).
     */
    static Snapshot withLegacyFallback(Snapshot fromAudit, QrLabel label) {
        Leg calidad = fromAudit.calidad();
        Leg inspeccion = fromAudit.inspeccion();
        if (!calidad.approved() && label.getCalidadApprovedAt() != null) {
            calidad = new Leg(true, null, label.getCalidadApprovedAt(), "CALIDAD");
        }
        if (!inspeccion.approved() && label.getInspeccionApprovedAt() != null) {
            inspeccion = new Leg(true, null, label.getInspeccionApprovedAt(), "INSPECCION");
        }
        return new Snapshot(calidad, inspeccion);
    }

    static String effectiveRole(AuditEvent e) {
        Map<String, Object> md = e.getMetadata();
        if (md != null) {
            Object rol = md.get("rol");
            if (rol == null) rol = md.get("approvalRole");
            if (rol != null) {
                String s = String.valueOf(rol).trim().toUpperCase(Locale.ROOT);
                if ("CALIDAD".equals(s) || "INSPECCION".equals(s)) return s;
            }
        }
        String actorRol = e.getActorRol();
        if (actorRol != null) {
            String s = actorRol.trim().toUpperCase(Locale.ROOT);
            if ("CALIDAD".equals(s) || "INSPECCION".equals(s)) return s;
        }
        return "";
    }

    static Map<String, Object> approveMetadata(
            QrLabel label,
            String tipo,
            String approvalRole,
            String resultingStatus,
            boolean calidadApproved,
            boolean inspeccionApproved,
            String motivo
    ) {
        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", label.getId().toString());
        md.put("tipoMaterial", tipo);
        md.put("rol", approvalRole);
        md.put("approvalRole", approvalRole);
        md.put("status", resultingStatus);
        md.put("resultingStatus", resultingStatus);
        md.put("calidadApproved", calidadApproved);
        md.put("inspeccionApproved", inspeccionApproved);
        if (motivo != null && !motivo.isBlank()) {
            md.put("motivo", motivo.trim());
        }
        return md;
    }

    static Map<String, Object> rejectMetadata(QrLabel label, String tipo, String motivo) {
        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", label.getId().toString());
        md.put("tipoMaterial", tipo);
        md.put("status", WorkflowStatus.RECHAZADO);
        md.put("resultingStatus", WorkflowStatus.RECHAZADO);
        if (motivo != null && !motivo.isBlank()) {
            md.put("motivo", motivo.trim());
        }
        return md;
    }
}
