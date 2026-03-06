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

        // Estado inicial fijo
        q.setStatusDinamico("PENDING");
        q.setCreatedAt(Instant.now());
        q.setPublicToken(java.util.UUID.randomUUID().toString().replace("-", ""));

        QrLabel saved;
        try {
            saved = repo.save(q);
        } catch (DataIntegrityViolationException ex) {
            // Por unique(lote) o unique(public_token)
            throw new ResponseStatusException(CONFLICT, "Ya existe una etiqueta con ese lote: " + lote);
        }

        String qrUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/qr/{id}")
                .buildAndExpand(saved.getId())
                .toUriString();

        return ResponseEntity.ok(new LabelDto.CreateResponse(
                saved.getId(),
                saved.getStatusDinamico(),
                qrUrl,
                saved.getPublicToken(),
                LabelDto.LabelView.from(saved)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','INSPECCION')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<LabelDto.StatusResponse> updateStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody LabelDto.StatusRequest req) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }

        String st = req.status().trim().toUpperCase(Locale.ROOT);
        if (!WorkflowStatus.isValid(st)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + st);
        }

        QrLabel q = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id));

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
            @RequestBody LabelDto.StatusRequest req) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }
        String st = req.status().trim().toUpperCase(Locale.ROOT);
        if (!WorkflowStatus.isValid(st)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + st);
        }
        QrLabel q = resolveLabel(lote == null ? "" : lote.trim());
        q.setStatusDinamico(st);
        repo.save(q);

        auditService.log(principal, "CHANGE_STATUS", q.getLote(),
                java.util.Map.of("status", st, "labelId", q.getId().toString()), null);

        return ResponseEntity.ok(new LabelDto.StatusResponse(q.getId(), q.getStatusDinamico()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<LabelDto.LabelView> getById(@PathVariable UUID id) {
        QrLabel q = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id));
        return ResponseEntity.ok(LabelDto.LabelView.from(q));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN')")
    @GetMapping("/{id}/zpl")
    public ResponseEntity<String> downloadZpl(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String id
    ) {
        String key = id == null ? "" : id.trim();
        QrLabel q = resolveLabel(key);

        String lote = q.getLote();
        String qrPayload = "OLNQR:1:" + safe(q.getPublicToken());

        String zpl =
                "^XA\n" +
                "^FO50,50^ADN,36,20^FDMaterial: " + safe(q.getNombre()) + "^FS\n" +
                "^FO50,100^FDCode: " + safe(q.getCodigo()) + "^FS\n" +
                "^FO50,150^FDLote: " + safe(lote) + "^FS\n" +
                "^FO50,200^FDEnvase: " + q.getEnvaseNum() + "/" + q.getEnvaseTotal() + "^FS\n" +
                "^FO50,260^BQN,2,6\n" +
                "^FDQA," + qrPayload + "^FS\n" +
                "^XZ\n";

        auditService.log(principal, "PRINT_LABEL", lote,
                java.util.Map.of(
                        "labelId", q.getId().toString(),
                        "lote", lote,
                        "mode", "ZPL_DOWNLOAD"
                ),
                null);

        return ResponseEntity
                .ok()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(zpl);
    }

    private QrLabel resolveLabel(String key) {
        if (key.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Identificador vacío");
        }
        try {
            UUID uuid = UUID.fromString(key);
            return repo.findById(uuid)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + key));
        } catch (IllegalArgumentException ignored) {
        }
        return repo.findByPublicToken(key)
                .or(() -> repo.findByLote(key))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada para: " + key));
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}