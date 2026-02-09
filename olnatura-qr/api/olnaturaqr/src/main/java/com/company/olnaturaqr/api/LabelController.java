package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public LabelController(QrLabelRepository repo) {
        this.repo = repo;
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

    // Solo ADMIN puede cambiar status
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<LabelDto.StatusResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody LabelDto.StatusRequest req
    ) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }

        String st = req.status().trim().toUpperCase(Locale.ROOT);
        if (!isValidStatus(st)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + st);
        }

        QrLabel q = repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id)
        );

        q.setStatusDinamico(st);
        repo.save(q);

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

    private static boolean isValidStatus(String st) {
        return st.equals("PENDING") || st.equals("CUARENTENA") || st.equals("APROBADO") || st.equals("RECHAZADO");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}