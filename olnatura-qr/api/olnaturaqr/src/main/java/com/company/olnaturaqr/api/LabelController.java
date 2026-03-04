package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/v1/label")
public class LabelController {

    private final QrLabelRepository repo;
    private final AuditService auditService;

    public LabelController(QrLabelRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    // ADMIN o ALMACEN pueden crear
    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN')")
    @PostMapping
    public ResponseEntity<LabelDto.CreateResponse> create(@RequestBody LabelDto.CreateRequest req) {

        if (isBlank(req.tipoMaterial()) || isBlank(req.nombre()) || isBlank(req.codigo()) || isBlank(req.lote())) {
            throw new ResponseStatusException(BAD_REQUEST, "Campos requeridos: tipoMaterial, nombre, codigo, lote");
        }
        if (req.fechaEntrada() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "fechaEntrada es requerida");
        }
        if (req.envaseNum() <= 0 || req.envaseTotal() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "envaseNum/envaseTotal deben ser > 0");
        }
        if (req.envaseNum() > req.envaseTotal()) {
            throw new ResponseStatusException(BAD_REQUEST, "envaseNum no puede ser mayor que envaseTotal");
        }

        String lote = req.lote().trim();
        String tipo = req.tipoMaterial().trim().toUpperCase(Locale.ROOT);

        QrLabel q = new QrLabel();
        q.setTipoMaterial(tipo);
        q.setNombre(req.nombre().trim());
        q.setCodigo(req.codigo().trim());
        q.setLote(lote);
        q.setFechaEntrada(req.fechaEntrada());
        q.setCaducidad(req.caducidad());
        q.setReanalisis(req.reanalisis());
        q.setEnvaseNum(req.envaseNum());
        q.setEnvaseTotal(req.envaseTotal());

        // ✅ Estado inicial fijo
        q.setStatusDinamico("PENDING");
        q.setCreatedAt(Instant.now());

        QrLabel saved;
        try {
            saved = repo.save(q);
        } catch (DataIntegrityViolationException ex) {
            // Por unique(lote)
            throw new ResponseStatusException(CONFLICT, "Ya existe una etiqueta con ese lote: " + lote);
        }

        // URL pública que irá dentro del QR
        String qrUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/qr/{id}")
                .buildAndExpand(saved.getId())
                .toUriString();

        return ResponseEntity.ok(new LabelDto.CreateResponse(
                saved.getId(),
                saved.getStatusDinamico(),
                qrUrl,
                LabelDto.LabelView.from(saved)
        ));
    }

    @PreAuthorize("hasAnyRole('ADMIN','INSPECCION')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<LabelDto.StatusResponse> updateStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody LabelDto.StatusRequest req
    ) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }

        String st = req.status().trim().toUpperCase(Locale.ROOT);
        if (!WorkflowStatus.isValid(st)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + st);
        }

        QrLabel q = repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id)
        );

        q.setStatusDinamico(st);
        repo.save(q);

        auditService.log(principal, "CHANGE_STATUS", q.getLote(),
                java.util.Map.of("status", st, "labelId", id.toString()), null);

        return ResponseEntity.ok(new LabelDto.StatusResponse(q.getId(), q.getStatusDinamico()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','INSPECCION')")
    @PatchMapping("/by-lote/{lote}/status")
    public ResponseEntity<LabelDto.StatusResponse> updateStatusByLote(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote,
            @RequestBody LabelDto.StatusRequest req
    ) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }
        String st = req.status().trim().toUpperCase(Locale.ROOT);
        if (!WorkflowStatus.isValid(st)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + st);
        }
        QrLabel q = repo.findByLote(lote.trim()).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada para lote: " + lote));
        q.setStatusDinamico(st);
        repo.save(q);

        auditService.log(principal, "CHANGE_STATUS", q.getLote(),
                java.util.Map.of("status", st, "labelId", q.getId().toString()), null);

        return ResponseEntity.ok(new LabelDto.StatusResponse(q.getId(), q.getStatusDinamico()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<LabelDto.LabelView> getById(@PathVariable UUID id) {
        QrLabel q = repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id)
        );
        return ResponseEntity.ok(LabelDto.LabelView.from(q));
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}