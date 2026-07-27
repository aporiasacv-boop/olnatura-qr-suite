package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corrección administrativa excepcional del estado del lote (solo ADMIN).
 * No sustituye el flujo normal de liberación. Solo cambia el status actual;
 * no altera historial de aprobaciones, comentarios ni bitácora.
 */
@Service
public class AdminStatusCorrectionService {

    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;

    public AdminStatusCorrectionService(QrLabelRepository qrLabelRepository, AuditService auditService) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
    }

    public record StatusCorrectionRequest(String status, String motivo) {}

    public record StatusCorrectionResult(QrLabel label, String from, String to, String motivo) {}

    /** Destinos permitidos desde el estado actual. */
    public static List<String> allowedTargets(String currentStatus) {
        String from = WorkflowStatus.normalize(currentStatus);
        if (WorkflowStatus.CUARENTENA.equals(from)) {
            return List.of(WorkflowStatus.APROBADO);
        }
        if (WorkflowStatus.APROBADO.equals(from)) {
            return List.of(WorkflowStatus.CUARENTENA);
        }
        if (WorkflowStatus.RECHAZADO.equals(from)) {
            return List.of(WorkflowStatus.CUARENTENA);
        }
        return Collections.emptyList();
    }

    public static boolean isAllowed(String fromRaw, String toRaw) {
        String from = WorkflowStatus.normalize(fromRaw);
        String to = WorkflowStatus.normalize(toRaw);
        if (!WorkflowStatus.isValid(toRaw == null ? "" : toRaw.trim())) {
            return false;
        }
        // RECHAZADO → APROBADO nunca.
        if (WorkflowStatus.RECHAZADO.equals(from) && WorkflowStatus.APROBADO.equals(to)) {
            return false;
        }
        return allowedTargets(from).contains(to);
    }

    @Transactional
    public StatusCorrectionResult correct(QrLabel label, AuthPrincipal principal, StatusCorrectionRequest req) {
        LotOperationalGate.requireActive(label);
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        }
        String motivo = req.motivo() == null ? "" : req.motivo().trim();
        if (motivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de la modificación es obligatorio");
        }
        if (motivo.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo supera 500 caracteres");
        }
        if (req.status() == null || req.status().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status es requerido");
        }
        String to = req.status().trim().toUpperCase();
        if (!WorkflowStatus.isValid(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado inválido. Usa: CUARENTENA, APROBADO o RECHAZADO");
        }

        String from = WorkflowStatus.normalize(label.getStatus());
        if (from.equals(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado ya es " + to);
        }
        if (WorkflowStatus.RECHAZADO.equals(from) && WorkflowStatus.APROBADO.equals(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se permite RECHAZADO → APROBADO. Debe pasar primero a CUARENTENA");
        }
        if (!isAllowed(from, to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transición administrativa no permitida: " + from + " → " + to
                            + ". Permitidas: CUARENTENA→APROBADO, APROBADO→CUARENTENA, RECHAZADO→CUARENTENA");
        }

        label.setStatus(to);
        QrLabel saved = qrLabelRepository.save(label);

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", saved.getId().toString());
        md.put("field", "status");
        md.put("fieldLabel", "Estado");
        md.put("from", from);
        md.put("to", to);
        md.put("motivo", motivo);
        md.put("rol", "ADMIN");
        md.put("correctionType", "ADMIN_STATUS");
        auditService.log(principal, "ADMIN_CORRECT_STATUS", saved.getLote(), md, null);

        return new StatusCorrectionResult(saved, from, to, motivo);
    }
}
