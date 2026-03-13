package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.MockDynamicsClient;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.support.zpl.OlnaturaLogoGfa;
import com.company.olnaturaqr.support.zpl.ZplGraphicUtil;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/v1/label")
public class LabelController {

    /** Microsoft Dynamics format: DD/MM/YYYY */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private final QrLabelRepository repo;
    private final AuditService auditService;
    private final MockDynamicsClient dynamics;

    public LabelController(QrLabelRepository repo, AuditService auditService, MockDynamicsClient dynamics) {
        this.repo = repo;
        this.auditService = auditService;
        this.dynamics = dynamics;
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
            @PathVariable String id,
            @RequestParam(required = false) Integer total,
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to
    ) {
        String key = id == null ? "" : id.trim();
        QrLabel q = resolveLabel(key);

        int envaseTotal = (total != null && total >= 1) ? total : q.getEnvaseTotal();
        int printFrom = (from != null && from >= 1) ? from : q.getEnvaseNum();
        int printTo = (to != null && to >= 1 && to <= envaseTotal) ? to : printFrom;

        if (printFrom > printTo) {
            throw new ResponseStatusException(BAD_REQUEST, "printFrom no puede ser mayor que printTo");
        }
        if (printFrom < 1 || printTo > envaseTotal) {
            throw new ResponseStatusException(BAD_REQUEST, "Rango debe estar entre 1 y " + envaseTotal);
        }

        StringBuilder zplAll = new StringBuilder();
        for (int seq = printFrom; seq <= printTo; seq++) {
            zplAll.append(buildSingleZpl(q, seq, envaseTotal, null));
        }

        String safeLote = loteSafe(q.getLote());
        String filename = (printFrom == printTo)
                ? "etiqueta-" + safeLote + ".zpl"
                : "etiqueta-" + safeLote + "-del-" + printFrom + "-al-" + printTo + ".zpl";

        auditService.log(principal, "PRINT_LABEL", q.getLote(),
                java.util.Map.of(
                        "labelId", q.getId().toString(),
                        "lote", q.getLote(),
                        "mode", "ZPL_DOWNLOAD",
                        "from", printFrom,
                        "to", printTo,
                        "count", printTo - printFrom + 1
                ),
                null);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(zplAll.toString());
    }

    /** POST: same as GET but accepts qrImageBase64 to embed QR+logo as ^GF graphic */
    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN')")
    @PostMapping(value = "/{id}/zpl", consumes = "application/json")
    public ResponseEntity<String> downloadZplWithGraphic(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String id,
            @RequestBody(required = false) LabelDto.ZplRequest req
    ) {
        Integer total = req != null && req.total() != null ? req.total() : null;
        Integer from = req != null && req.from() != null ? req.from() : null;
        Integer to = req != null && req.to() != null ? req.to() : null;
        String qrBase64 = req != null && req.qrImageBase64() != null && !req.qrImageBase64().isBlank()
                ? req.qrImageBase64() : null;

        String key = id == null ? "" : id.trim();
        QrLabel q = resolveLabel(key);

        int envaseTotal = (total != null && total >= 1) ? total : q.getEnvaseTotal();
        int printFrom = (from != null && from >= 1) ? from : q.getEnvaseNum();
        int printTo = (to != null && to >= 1 && to <= envaseTotal) ? to : printFrom;

        if (printFrom > printTo) {
            throw new ResponseStatusException(BAD_REQUEST, "printFrom no puede ser mayor que printTo");
        }
        if (printFrom < 1 || printTo > envaseTotal) {
            throw new ResponseStatusException(BAD_REQUEST, "Rango debe estar entre 1 y " + envaseTotal);
        }

        StringBuilder zplAll = new StringBuilder();
        for (int seq = printFrom; seq <= printTo; seq++) {
            zplAll.append(buildSingleZpl(q, seq, envaseTotal, qrBase64));
        }

        String safeLote = loteSafe(q.getLote());
        String filename = (printFrom == printTo)
                ? "etiqueta-" + safeLote + ".zpl"
                : "etiqueta-" + safeLote + "-del-" + printFrom + "-al-" + printTo + ".zpl";

        auditService.log(principal, "PRINT_LABEL", q.getLote(),
                java.util.Map.of(
                        "labelId", q.getId().toString(),
                        "lote", q.getLote(),
                        "mode", "ZPL_DOWNLOAD",
                        "from", printFrom,
                        "to", printTo,
                        "count", printTo - printFrom + 1
                ),
                null);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(zplAll.toString());
    }

    private String loteSafe(String lote) {
        return (lote == null ? "label" : lote).replaceAll("[\\s/\\\\]+", "_");
    }

    private String buildSingleZpl(QrLabel q, int envaseNum, int envaseTotal, String qrImageBase64) {
        String lote = q.getLote();
        String qrPayload = "OLNQR:1:" + safe(q.getPublicToken());
        String tipoMaterial = orEmpty(q.getTipoMaterial(), "MATERIAL DE ACONDICIONADO");
        String nombre = safe(q.getNombre());
        String codigo = safe(q.getCodigo());
        String fechaStr = formatDate(q.getFechaEntrada());
        String caducidadStr = formatDate(q.getCaducidad());
        String reanalisisStr = q.getReanalisis() != null ? formatDate(q.getReanalisis()) : "N/A";
        String cantidadStr = dynamics.fetchByLote(lote)
                .map(d -> String.format("%.0f", d.cantidad()).replace(".0", "") + (d.uom() != null && !d.uom().isBlank() ? " " + d.uom().trim() : ""))
                .orElse("N/A");

        return "^XA\n" +
                "^PW800\n" +
                "^LL600\n" +
                "^CI28\n" +
                "\n" +
                "^FO20,30^GB760,2,2^FS\n" +
                "^FO20,90^GB760,2,2^FS\n" +
                "^FO20,160^GB760,2,2^FS\n" +
                "^FO20,560^GB760,2,2^FS\n" +
                "\n" +
                "^FO20,30^GB2,530,2^FS\n" +
                "^FO778,30^GB2,530,2^FS\n" +
                "\n" +
                "^FO20,45^A0N,32,32^FB760,1,0,C,0^FD" + escapeZpl(tipoMaterial) + "^FS\n" +
                "\n" +
                "^FO30,105^A0N,24,24^FDNombre:^FS\n" +
                "^FO135,103^A0N,25,25^FB620,2,2,L,0^FD" + escapeZpl(nombre) + "^FS\n" +
                "\n" +
                "^FO20,160^GB220,80,2^FS\n" +
                "^FO240,160^GB250,80,2^FS\n" +
                "^FO490,160^GB290,80,2^FS\n" +
                "\n" +
                "^FO30,175^A0N,22,22^FDFecha:^FS\n" +
                "^FO30,203^A0N,28,28^FD" + escapeZpl(fechaStr) + "^FS\n" +
                "\n" +
                "^FO250,175^A0N,22,22^FDCodigo:^FS\n" +
                "^FO250,203^A0N,28,28^FD" + escapeZpl(codigo) + "^FS\n" +
                "\n" +
                "^FO500,175^A0N,22,22^FDLote:^FS\n" +
                "^FO500,203^A0N,24,24^FD" + escapeZpl(lote) + "^FS\n" +
                "\n" +
                "^FO20,240^GB430,190,2^FS\n" +
                "^FO450,240^GB330,320,3^FS\n" +
                "\n" +
                "^FO35,270^A0N,24,24^FDCaducidad:^FS\n" +
                "^FO210,270^A0N,30,30^FD" + escapeZpl(caducidadStr) + "^FS\n" +
                "\n" +
                "^FO35,320^A0N,24,24^FDReanalisis:^FS\n" +
                "^FO210,320^A0N,30,30^FD" + escapeZpl(reanalisisStr) + "^FS\n" +
                "\n" +
                "^FO35,370^A0N,24,24^FDCantidad:^FS\n" +
                "^FO210,370^A0N,30,30^FD" + escapeZpl(cantidadStr) + "^FS\n" +
                "\n" +
                "^FO20,425^GB260,135,2^FS\n" +
                "^FO280,425^GB170,135,2^FS\n" +
                "\n" +
                "^FO35,450^A0N,22,22^FDEnvase No.^FS\n" +
                "^FO70,490^A0N,44,44^FD" + String.format("%02d", envaseNum) + "^FS\n" +
                "^FO138,500^A0N,24,24^FDde^FS\n" +
                "^FO180,490^A0N,44,44^FD" + String.format("%02d", envaseTotal) + "^FS\n" +
                "\n" +
                "^FO300,450^A0N,20,20^FDCantidad total^FS\n" +
                "^FO345,495^A0N,48,48^FD" + envaseTotal + "^FS\n" +
                "\n" +
                qrBlock(qrImageBase64, qrPayload) +
                "\n" +
                "^XZ\n";
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

    private String orEmpty(String v, String fallback) {
        return (v == null || v.trim().isEmpty()) ? fallback : v.trim();
    }

    private String formatDate(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "N/A";
    }

    private String qrBlock(String qrImageBase64, String qrPayload) {
        if (qrImageBase64 != null && !qrImageBase64.isBlank()) {
            String gfa = ZplGraphicUtil.toGfa(qrImageBase64, 200);
            if (!gfa.isEmpty()) {
                return "^FO485,260\n" + gfa + "\n^FS";
            }
        }
        // Native Zebra ^BQN QR + Olnatura logo overlay (white rect + logo centered)
        int qrX = 485;
        int qrY = 260;
        int logoSize = 40;
        int qrCenterOffset = 25;  // ~half of typical QR size with ^BQN,2,8
        int overlayX = qrX + qrCenterOffset - logoSize / 2;
        int overlayY = qrY + qrCenterOffset - logoSize / 2;
        String qrZpl = "^FO" + qrX + "," + qrY + "^BQN,2,8\n^FDQA," + qrPayload + "^FS";
        String whiteRect = OlnaturaLogoGfa.whiteRectGfa(logoSize);
        String logoGfa = OlnaturaLogoGfa.smallOverlayGfa();
        String logoOverlay = "^FO" + overlayX + "," + overlayY + "\n" + whiteRect + "\n^FS\n" +
                "^FO" + overlayX + "," + overlayY + "\n" + logoGfa + "\n^FS";
        return qrZpl + "\n" + logoOverlay;
    }

    /** Escape ^ and \ to avoid breaking ZPL field commands */
    private String escapeZpl(String s) {
        if (s == null) return "";
        return s.replace("\\", " ").replace("^", " ");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}