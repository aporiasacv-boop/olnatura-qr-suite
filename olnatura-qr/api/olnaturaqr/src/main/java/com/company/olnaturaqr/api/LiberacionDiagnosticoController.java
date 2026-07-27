package com.company.olnaturaqr.api;

import com.company.olnaturaqr.support.diagnostics.LiberacionDiagnosticoDtos;
import com.company.olnaturaqr.support.diagnostics.LiberacionDiagnosticoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Captura evidencia Dynamics de un lote y la guarda en diagnostics/{lote}_{fase}.json.
 * No compara, no sincroniza, no modifica estado QR.
 *
 * Ejemplo:
 *   POST /api/v1/diagnostics/liberacion/260717-MEM0003668?fase=ANTES
 *   POST /api/v1/diagnostics/liberacion/260717-MEM0003668?fase=DESPUES
 */
@RestController
@RequestMapping("/api/v1/diagnostics/liberacion")
public class LiberacionDiagnosticoController {

    private final LiberacionDiagnosticoService service;

    public LiberacionDiagnosticoController(LiberacionDiagnosticoService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @PostMapping("/{lote}")
    public LiberacionDiagnosticoDtos.Captura capturar(
            @PathVariable String lote,
            @RequestParam(defaultValue = "ANTES") String fase
    ) {
        return service.capturarYGuardar(lote, fase);
    }
}
