package com.company.olnaturaqr.api;

import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.support.workflow.OperationalStatusPresentation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;


@RestController
@RequestMapping("/api/v1/dynamics")
public class DynamicsLookupController {

    private final DynamicsLookupService dynamicsLookupService;

    public DynamicsLookupController(DynamicsLookupService dynamicsLookupService) {
        this.dynamicsLookupService = dynamicsLookupService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN','PRODUCCION','CALIDAD','INSPECCION')")
    @GetMapping("/lookup/{lote}")
    public DynamicsLookupDto lookupByLote(@PathVariable String lote) {
        DynamicsLookupDto dto = dynamicsLookupService.lookupByBatchNumber(lote)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Lote no encontrado en Dynamics: " + lote.trim()
                ));
        return new DynamicsLookupDto(
                dto.codigo(),
                dto.nombre(),
                dto.lote(),
                dto.caducidad(),
                dto.cantidadAlmacen(),
                dto.unidadInventario(),
                dto.fechaEntrada(),
                OperationalStatusPresentation.forUi(dto.operationalStatus()),
                dto.operationalStatusRule(),
                dto.statusSource(),
                dto.statusDynamics(),
                dto.qualityOrderStatus(),
                dto.passedBatchDispositionCode(),
                dto.batchDispositionCode(),
                dto.almacen(),
                dto.ubicacion(),
                dto.fuente(),
                dto.fechaLiberacion(),
                dto.liberadoPor(),
                dto.warehouses()
        );
    }
}