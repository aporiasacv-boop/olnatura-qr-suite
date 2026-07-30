package com.company.olnaturaqr.api.temp;

import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public OperationalStatusValidationController(DynamicsLookupService dynamicsLookupService) {
        this.dynamicsLookupService = dynamicsLookupService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN','PRODUCCION','CALIDAD','INSPECCION')")
    @GetMapping("/{lote}")
    public OperationalStatusValidationResponse validate(@PathVariable String lote) {
        DynamicsLookupDto dto = dynamicsLookupService.lookupByBatchNumber(lote)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Lote no encontrado en Dynamics: " + (lote == null ? "" : lote.trim())
                ));

        return new OperationalStatusValidationResponse(
                dto.lote(),
                dto.codigo(),
                dto.nombre(),
                dto.caducidad(),
                dto.cantidadAlmacen(),
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
