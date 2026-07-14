package com.company.olnaturaqr.support.qr;

import com.company.olnaturaqr.api.QrDto;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.support.workflow.WorkflowTransitions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class QrQueryService {

    private final QrLabelRepository qrLabelRepository;
    private final DynamicsLookupService dynamicsLookupService;

    public QrQueryService(QrLabelRepository qrLabelRepository, DynamicsLookupService dynamicsLookupService) {
        this.qrLabelRepository = qrLabelRepository;
        this.dynamicsLookupService = dynamicsLookupService;
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

        // Platform-owned status only — never derived from Dynamics QualityOrderStatus.
        String platformStatus = WorkflowStatus.normalize(label.getStatus());

        QrDto.Dynamic dyn = lookupDynamicsOrFail(lote)
                .map(d -> toDynamicDto(d, platformStatus))
                .orElseGet(() -> new QrDto.Dynamic(
                        label.getCodigo(),
                        label.getNombre(),
                        lote,
                        label.getCaducidad() != null ? label.getCaducidad().toString() : null,
                        null,
                        platformStatus,
                        null,
                        null,
                        null,
                        "DB_ONLY"
                ));

        List<String> transitions = principal != null
                ? WorkflowTransitions.allowedFrom(platformStatus)
                : Collections.emptyList();
        QrDto.Permissions perms = buildPermissions(principal);

        return new QrDto.Response(dtoLabel, dyn, transitions, perms);
    }

    private Optional<DynamicsLookupDto> lookupDynamicsOrFail(String lote) {
        // DynamicsException (OAuth/OData/timeout/interno) propaga al GlobalExceptionHandler.
        return dynamicsLookupService.lookupByBatchNumber(lote);
    }

    private static QrDto.Dynamic toDynamicDto(DynamicsLookupDto d, String platformStatus) {
        return new QrDto.Dynamic(
                d.codigo(),
                d.nombre(),
                d.lote(),
                d.caducidad(),
                d.cantidadAlmacen(),
                platformStatus,
                d.statusDynamics(),
                d.almacen(),
                d.ubicacion(),
                d.fuente()
        );
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
}
