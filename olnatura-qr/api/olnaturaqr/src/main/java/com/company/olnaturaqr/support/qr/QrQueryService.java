package com.company.olnaturaqr.support.qr;

import com.company.olnaturaqr.api.QrDto;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.MockDynamicsClient;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.WorkflowTransitions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class QrQueryService {

    private final QrLabelRepository qrLabelRepository;
    private final MockDynamicsClient dynamics;

    public QrQueryService(QrLabelRepository qrLabelRepository, MockDynamicsClient dynamics) {
        this.qrLabelRepository = qrLabelRepository;
        this.dynamics = dynamics;
    }

    @Transactional(readOnly = true)
    public QrDto.Response getByLote(String loteRaw, AuthPrincipal principal) {
        String normalized = LoteExtractor.extract(loteRaw)
                .orElse(loteRaw != null ? loteRaw.trim() : "");

        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Lote requerido");
        }
        if (normalized.length() > 120) {
            throw new ResponseStatusException(BAD_REQUEST, "Lote demasiado largo");
        }

        String lote = normalized;
        QrLabel label = qrLabelRepository.findByLote(lote)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + lote));

        var dtoLabel = new QrDto.Label(
                label.getTipoMaterial(),
                label.getNombre(),
                label.getCodigo(),
                label.getLote(),
                label.getFechaEntrada(),
                label.getCaducidad(),
                label.getReanalisis(),
                label.getEnvaseNum(),
                label.getEnvaseTotal()
        );

        String statusOverride = normalizeStatus(label.getStatusDinamico());
        boolean useDbStatus = statusOverride != null && !"DESCONOCIDO".equals(statusOverride);

        var dyn = dynamics.fetchByLote(lote)
                .map(d -> new QrDto.Dynamic(
                        useDbStatus ? statusOverride : d.status(),
                        d.cantidad(),
                        d.uom(),
                        d.ubicacion(),
                        d.fuente()
                ))
                .orElseGet(() -> new QrDto.Dynamic(
                        statusOverride,
                        null,
                        null,
                        null,
                        "DB_ONLY"
                ));

        String currentStatus = dyn.status();
        List<String> transitions = principal != null
                ? WorkflowTransitions.allowedFrom(currentStatus)
                : Collections.emptyList();
        QrDto.Permissions perms = buildPermissions(principal);

        return new QrDto.Response(dtoLabel, dyn, transitions, perms);
    }

    private QrDto.Permissions buildPermissions(AuthPrincipal principal) {
        if (principal == null || principal.roles() == null) {
            return new QrDto.Permissions(false, false, false);
        }
        var roles = principal.roles();
        boolean canChangeStatus = roles.contains("ADMIN") || roles.contains("INSPECCION");
        boolean canCreateLabel = roles.contains("ADMIN") || roles.contains("ALMACEN");
        boolean canRegisterScan = roles.contains("ADMIN") || roles.contains("INSPECCION") || roles.contains("ALMACEN");
        return new QrDto.Permissions(canChangeStatus, canRegisterScan, canCreateLabel);
    }

    private String normalizeStatus(String s) {
        if (s == null) return "DESCONOCIDO";
        var v = s.trim().toUpperCase();
        return v.isBlank() ? "DESCONOCIDO" : v;
    }
}