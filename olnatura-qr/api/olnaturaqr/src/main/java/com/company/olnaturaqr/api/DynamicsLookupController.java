package com.company.olnaturaqr.api;

import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Consulta Dynamics por BatchNumber sin exigir etiqueta previa en la base local.
 * Usado para precargar el formulario de registro de etiqueta.
 */
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
        return dynamicsLookupService.lookupByBatchNumber(lote)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Lote no encontrado en Dynamics: " + lote.trim()
                ));
    }
}
