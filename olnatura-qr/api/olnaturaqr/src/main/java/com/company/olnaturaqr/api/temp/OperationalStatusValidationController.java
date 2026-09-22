package com.company.olnaturaqr.api.temp;

import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.OperationalStatusSyncService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/temp/operational-status-validation")
public class OperationalStatusValidationController {

    private final DynamicsLookupService dynamicsLookupService;
    private final QrLabelRepository qrLabelRepository;
    private final OperationalStatusSyncService operationalStatusSyncService;

    public OperationalStatusValidationController(
            DynamicsLookupService dynamicsLookupService,
            QrLabelRepository qrLabelRepository,
            OperationalStatusSyncService operationalStatusSyncService
    ) {
        this.dynamicsLookupService = dynamicsLookupService;
        this.qrLabelRepository = qrLabelRepository;
        this.operationalStatusSyncService = operationalStatusSyncService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN','PRODUCCION','CALIDAD','INSPECCION','VALIDACION')")
    @GetMapping("/{lote}")
    public OperationalStatusValidationResponse validate(
            @PathVariable String lote,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        DynamicsLookupDto dto = dynamicsLookupService.lookupByBatchNumber(lote)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Lote no encontrado en Dynamics: " + (lote == null ? "" : lote.trim())
                ));

        // Ante diferencia, Dynamics prevalece y se actualiza la copia en qr_labels.
        String loteKey = dto.lote() != null && !dto.lote().isBlank() ? dto.lote().trim() : (lote == null ? "" : lote.trim());
        if (!loteKey.isBlank()) {
            qrLabelRepository.findByLote(loteKey).ifPresent(label ->
                    operationalStatusSyncService.applyDynamicsStatus(
                            label,
                            dto.operationalStatus(),
                            OperationalStatusSyncService.REASON_CONSULTA,
                            principal
                    )
            );
        }

        return new OperationalStatusValidationResponse(
                dto.lote(),
                dto.codigo(),
                dto.nombre(),
                dto.caducidad(),
                dto.cantidadAlmacen(),
                dto.cantidadRecibida(),
                dto.unidadInventario(),
                dto.fechaEntrada(),
                dto.almacen(),
                dto.ubicacion(),
                dto.fuente(),
                dto.statusDynamics(),
                dto.qualityOrderStatus(),
                dto.passedBatchDispositionCode(),
                dto.batchDispositionCode(),
                dto.warehouses() != null ? dto.warehouses() : java.util.List.of(),
                dto.fechaLiberacion(),
                dto.liberadoPor(),
                dto.operationalStatus(),
                dto.operationalStatusRule(),
                dto.statusSource(),
                OperationalStatusValidationExplainer.explain(dto)
        );
    }
}
