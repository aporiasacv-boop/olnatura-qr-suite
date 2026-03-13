package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.MockDynamicsClient;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
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
        q.setDocumentCode(req.documentCode() != null && !req.documentCode().isBlank() ? req.documentCode().trim() : null);

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
        String documentCode = orEmpty(q.getDocumentCode(), "AL-001-E02/04");
        String envaseDisplay = String.format("%02d", envaseNum) + " de " + String.format("%02d", envaseTotal);

        return "^XA\n" +
                "^PW800\n" +
                "^LL600\n" +
                "^CI15\n" +
                "\n" +
                "^FO8,8^GB790,590,9^FS\n" +
                "\n" +
                "^FO20,20^GB90,100,2^FS\n" +
                "^FO110,20^GB670,50,2^FS\n" +
                "^FO110,70^GB670,50,2^FS\n" +
                "\n" +
                "^FO20,120^GB130,65,2^FS\n" +
                "^FO150,120^GB230,65,2^FS\n" +
                "^FO380,120^GB400,65,2^FS\n" +
                "\n" +
                "^FO20,185^GB360,70,2^FS\n" +
                "^FO20,255^GB360,70,2^FS\n" +
                "^FO20,325^GB360,70,2^FS\n" +
                "\n" +
                "^FO380,185^GB400,300,2^FS\n" +
                "\n" +
                "^FO20,395^GB180,90,2^FS\n" +
                "^FO200,395^GB180,90,2^FS\n" +
                "\n" +
                "^FO20,485^GB760,95,2^FS\n" +
                "\n" +
                "^FO25,25\n^GFA,1080,1080,12,0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001C00000000000000000000000FC000000000000000003FFE07F80000000000000001FFFFC7FE0000000000000007FFFFC3FF800000000000001FFFFFE3FFE00000000000007FFFFFE3FFF8000000000000FFFFFFE1FFFC000000000001FFFFFFF1FFFE000000000003FFFFFFF1FFFF000000000007FFE003F1FFFF80000000000FFF000070FFFF80000000001FFE000018FFFFC0000000003FF8000008FFFFE0000000003FF00000007FFFE0000000007FE00000007FFFE0000000007FC00000003FFFF000000000FFC00000001FFFF000000000FF800000001FFFF000000000FF8000000007FFF000000001FF0000000003FFF000000001FF0000000020FFF000000001FF00000000101FF000000001FF000000001C01C000000001FE000000001F000000000001FE000000001FE00000000001FE000000001FE00000000001FE000000001FE00000000001FE000000001FE00000000001FE000000001FE00000000001FF000000001FE00000000001FF000000001FE00000000001FF000000001FE00000000001FF000000003FE00000000000FF800000003FC00000000000FF800000007FC00000000000FFC00000007FC000000000007FC0000000FFC000000000007FE0000000FF8000000000003FF0000001FF8000000000003FF8000003FF0000000000001FFC000007FF0000000000001FFF00001FFE0000000000000FFF80007FFC00000000000007FFF001FFFC00000000000003FFFFFFFFF800000000000001FFFFFFFFF000000000000000FFFFFFFFE0000000000000007FFFFFFF80000000000000001FFFFFFF000000000000000007FFFFFC000000000000000001FFFFE00000000000000000001FFF000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000^FS\n" +
                "\n" +
                "^FO125,40^A0N,28,28^FD" + escapeZpl(tipoMaterial) + "^FS\n" +
                "^FO130,92^A0N,28,28^FD" + escapeZpl(nombre) + "^FS\n" +
                "\n" +
                "^FO28,125^A0N,24,24^FD" + escapeZpl(fechaStr) + "^FS\n" +
                "^FO158,125^A0N,24,24^FD" + escapeZpl(codigo) + "^FS\n" +
                "^FO388,125^A0N,24,24^FD" + escapeZpl(lote) + "^FS\n" +
                "\n" +
                "^FO28,208^A0N,24,24^FD" + escapeZpl(caducidadStr) + "^FS\n" +
                "^FO28,278^A0N,24,24^FD" + escapeZpl(reanalisisStr) + "^FS\n" +
                "^FO28,348^A0N,24,24^FD" + escapeZpl(cantidadStr) + "^FS\n" +
                "\n" +
                qrBlock(qrImageBase64, qrPayload) + "\n" +
                "\n" +
                "^FO28,400^A0N,24,24^FD" + escapeZpl(envaseDisplay) + "^FS\n" +
                "^FO208,400^A0N,24,24^FD" + envaseTotal + "^FS\n" +
                "\n" +
                "^FO25,510^A0N,16,16^FB740,3,3,L,0^FD" + escapeZpl(documentCode) + " Propiedad de Olnatura S.A. de C.V. Prohibido su uso, divulgacion y/o reproduccion total o parcial. Si este documento no se encuentra controlado, se considera COPIA SOLO PARA INFORMACION.^FS\n" +
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
        // Always use native Zebra QR (^BQN). qrImageBase64 is ignored; ^GFA bitmap QR is not used.
        return "^FO485,260^BQN,2,8\n^FDQA," + qrPayload + "^FS";
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