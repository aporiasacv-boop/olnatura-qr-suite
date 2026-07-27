package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.util.SpanishFlexibleDateParser;
import com.company.olnaturaqr.support.workflow.AdminLotStatus;
import com.company.olnaturaqr.support.workflow.ApprovalService;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
import com.company.olnaturaqr.support.workflow.MaterialType;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.support.zpl.ZplTextNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/v1/label")
public class LabelController {


    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final Charset ZPL_OUT_CHARSET = StandardCharsets.ISO_8859_1;

    private final QrLabelRepository repo;
    private final AuditService auditService;
    private final ApprovalService approvalService;

    public LabelController(QrLabelRepository repo, AuditService auditService, ApprovalService approvalService) {
        this.repo = repo;
        this.auditService = auditService;
        this.approvalService = approvalService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN')")
    @PostMapping
    public ResponseEntity<LabelDto.CreateResponse> create(@RequestBody LabelDto.CreateRequest req) {

        if (isBlank(req.tipoMaterial()) || isBlank(req.nombre()) || isBlank(req.codigo()) || isBlank(req.lote())) {
            throw new ResponseStatusException(BAD_REQUEST, "Campos requeridos: tipoMaterial, nombre, codigo, lote");
        }
        if (req.fechaEntrada() == null || req.fechaEntrada().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "fechaEntrada es requerida");
        }
        if (req.envaseNum() <= 0 || req.envaseTotal() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "envaseNum/envaseTotal deben ser > 0");
        }
        if (req.envaseNum() > req.envaseTotal()) {
            throw new ResponseStatusException(BAD_REQUEST, "envaseNum no puede ser mayor que envaseTotal");
        }

        String lote = req.lote().trim();
        String tipo = MaterialType.normalize(req.tipoMaterial());
        if (!MaterialType.isValid(tipo)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "tipoMaterial inválido. Usa: Materia Prima, Material de Empaque Primario o Material de Empaque Secundario");
        }

        QrLabel q = new QrLabel();
        q.setTipoMaterial(tipo);
        q.setNombre(req.nombre().trim());
        q.setCodigo(req.codigo().trim());
        q.setLote(lote);
        q.setFechaEntrada(SpanishFlexibleDateParser.parseRequired(req.fechaEntrada(), "fechaEntrada"));
        q.setCaducidad(SpanishFlexibleDateParser.parseOptional(req.caducidad()));
        q.setReanalisis(SpanishFlexibleDateParser.parseOptional(req.reanalisis()));
        q.setEnvaseNum(req.envaseNum());
        q.setEnvaseTotal(req.envaseTotal());
        String cpe = req.cantidadPorEnvase() != null ? req.cantidadPorEnvase().trim() : "";
        q.setCantidadPorEnvase(cpe.isEmpty() ? null : cpe);
        q.setDocumentCode(
                req.documentCode() != null && !req.documentCode().isBlank() ? req.documentCode().trim() : null);

        q.setStatus(WorkflowStatus.CUARENTENA);
        q.setAdminStatus(AdminLotStatus.ACTIVE);
        q.setCreatedAt(Instant.now());
        q.setPublicToken(java.util.UUID.randomUUID().toString().replace("-", ""));

        QrLabel saved;
        try {
            saved = repo.save(q);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Ya existe una etiqueta con ese lote: " + lote);
        }

        String qrUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/qr/{id}")
                .buildAndExpand(saved.getId())
                .toUriString();

        return ResponseEntity.ok(new LabelDto.CreateResponse(
                saved.getId(),
                saved.getStatus(),
                qrUrl,
                saved.getPublicToken(),
                LabelDto.LabelView.from(saved)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @PostMapping("/by-lote/{lote}/approve")
    public ResponseEntity<LabelDto.StatusResponse> approveByLote(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote,
            @RequestBody(required = false) LabelDto.DecisionRequest body
    ) {
        QrLabel q = resolveLabel(lote == null ? "" : lote.trim());
        String motivo = body != null ? body.motivo() : null;
        QrLabel saved = approvalService.approve(q, principal, motivo);
        return ResponseEntity.ok(new LabelDto.StatusResponse(saved.getId(), saved.getStatus()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @PostMapping("/by-lote/{lote}/reject")
    public ResponseEntity<LabelDto.StatusResponse> rejectByLote(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote,
            @RequestBody(required = false) LabelDto.DecisionRequest body
    ) {
        QrLabel q = resolveLabel(lote == null ? "" : lote.trim());
        String motivo = body != null ? body.motivo() : null;
        QrLabel saved = approvalService.reject(q, principal, motivo);
        return ResponseEntity.ok(new LabelDto.StatusResponse(saved.getId(), saved.getStatus()));
    }

    /** Compatibilidad: APROBADO/RECHAZADO delegan al flujo de aprobación. */
    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<LabelDto.StatusResponse> updateStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody LabelDto.StatusRequest req) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }
        String st = WorkflowStatus.normalize(req.status());
        QrLabel q = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id));
        QrLabel saved;
        if (WorkflowStatus.APROBADO.equals(st)) {
            saved = approvalService.approve(q, principal, req.motivo());
        } else if (WorkflowStatus.RECHAZADO.equals(st)) {
            saved = approvalService.reject(q, principal, req.motivo());
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Usa APROBADO o RECHAZADO");
        }
        return ResponseEntity.ok(new LabelDto.StatusResponse(saved.getId(), saved.getStatus()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @PatchMapping("/by-lote/{lote}/status")
    public ResponseEntity<LabelDto.StatusResponse> updateStatusByLote(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote,
            @RequestBody LabelDto.StatusRequest req) {
        if (req == null || isBlank(req.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "status es requerido");
        }
        String st = WorkflowStatus.normalize(req.status());
        QrLabel q = resolveLabel(lote == null ? "" : lote.trim());
        QrLabel saved;
        if (WorkflowStatus.APROBADO.equals(st)) {
            saved = approvalService.approve(q, principal, req.motivo());
        } else if (WorkflowStatus.RECHAZADO.equals(st)) {
            saved = approvalService.reject(q, principal, req.motivo());
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Usa APROBADO o RECHAZADO");
        }
        return ResponseEntity.ok(new LabelDto.StatusResponse(saved.getId(), saved.getStatus()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<LabelDto.LabelView> getById(@PathVariable UUID id) {
        QrLabel q = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + id));
        return ResponseEntity.ok(LabelDto.LabelView.from(q));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN','PRODUCCION','CALIDAD','INSPECCION')")
    @GetMapping("/{id}/zpl")
    public ResponseEntity<byte[]> downloadZpl(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String id,
            @RequestParam(required = false) Integer total,
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to) {
        String key = id == null ? "" : id.trim();
        QrLabel q = resolveLabel(key);

        int[] bounds = resolveReprintBounds(q, total, from, to);
        int envaseTotal = bounds[0];
        int printFrom = bounds[1];
        int printTo = bounds[2];

        // Una sola resolución de cantidad por solicitud (solo BD; sin Dynamics).
        String cantidadStr = resolveCantidadForZpl(q);

        StringBuilder zplAll = new StringBuilder();
        for (int seq = printFrom; seq <= printTo; seq++) {
            zplAll.append(buildSingleZpl(q, seq, envaseTotal, null, cantidadStr));
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
                        "count", printTo - printFrom + 1),
                null);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        String zplText = zplAll.toString();
        byte[] zplBytes = zplText.getBytes(ZPL_OUT_CHARSET);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(new MediaType("text", "plain", ZPL_OUT_CHARSET))
                .body(zplBytes);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALMACEN','PRODUCCION','CALIDAD','INSPECCION')")
    @PostMapping(value = "/{id}/zpl", consumes = "application/json")
    public ResponseEntity<byte[]> downloadZplWithGraphic(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String id,
            @RequestBody(required = false) LabelDto.ZplRequest req) {
        Integer total = req != null && req.total() != null ? req.total() : null;
        Integer from = req != null && req.from() != null ? req.from() : null;
        Integer to = req != null && req.to() != null ? req.to() : null;
        String qrBase64 = req != null && req.qrImageBase64() != null && !req.qrImageBase64().isBlank()
                ? req.qrImageBase64()
                : null;

        String key = id == null ? "" : id.trim();
        QrLabel q = resolveLabel(key);

        int[] bounds = resolveReprintBounds(q, total, from, to);
        int envaseTotal = bounds[0];
        int printFrom = bounds[1];
        int printTo = bounds[2];

        // Una sola resolución de cantidad por solicitud (solo BD; sin Dynamics).
        String cantidadStr = resolveCantidadForZpl(q);

        StringBuilder zplAll = new StringBuilder();
        for (int seq = printFrom; seq <= printTo; seq++) {
            zplAll.append(buildSingleZpl(q, seq, envaseTotal, qrBase64, cantidadStr));
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
                        "count", printTo - printFrom + 1),
                null);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        String zplText = zplAll.toString();
        byte[] zplBytes = zplText.getBytes(ZPL_OUT_CHARSET);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(new MediaType("text", "plain", ZPL_OUT_CHARSET))
                .body(zplBytes);
    }

    /**
     * Reimpresión: el total es siempre el registrado en BD.
     * No se permite aumentar (ni alterar) envases; from/to deben estar en 1..total.
     *
     * @return int[]{envaseTotal, printFrom, printTo}
     */
    private int[] resolveReprintBounds(QrLabel q, Integer totalParam, Integer from, Integer to) {
        int registeredTotal = Math.max(1, q.getEnvaseTotal());
        if (totalParam != null && totalParam >= 1 && totalParam != registeredTotal) {
            if (totalParam > registeredTotal) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "No se puede aumentar el total de envases. Este lote tiene "
                                + registeredTotal + " registrados.");
            }
            throw new ResponseStatusException(BAD_REQUEST,
                    "El total de envases debe coincidir con el registrado (" + registeredTotal + ").");
        }

        int printFrom = (from != null) ? from : 1;
        int printTo = (to != null) ? to : registeredTotal;

        if (printFrom < 1 || printTo < 1) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "El rango debe comenzar en 1. Este lote tiene " + registeredTotal + " envase(s).");
        }
        if (printFrom > printTo) {
            throw new ResponseStatusException(BAD_REQUEST, "Desde no puede ser mayor que Hasta");
        }
        if (printFrom > registeredTotal || printTo > registeredTotal) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Rango inválido: solo existen etiquetas del 1 al " + registeredTotal
                            + " para este lote.");
        }
        return new int[]{registeredTotal, printFrom, printTo};
    }

    /**
     * Cantidad impresa en etiqueta: solo captura manual en BD.
     * El inventario Dynamics NO va en ZPL; se muestra al escanear el QR.
     */
    private String resolveCantidadForZpl(QrLabel q) {
        String manualQty = safe(q.getCantidadPorEnvase());
        return manualQty.isEmpty() ? "N/A" : manualQty;
    }

    private String loteSafe(String lote) {
        return (lote == null ? "label" : lote).replaceAll("[\\s/\\\\]+", "_");
    }

    private String buildSingleZpl(QrLabel q, int envaseNum, int envaseTotal, String qrImageBase64, String cantidadStr) {
        // Normalizacion unica: todo texto al ZPL es ASCII sin acentos ni glifos raros.
        String lote = ZplTextNormalizer.normalize(q.getLote());
        String qrPayload = "OLNQR:1:" + ZplTextNormalizer.normalize(safe(q.getPublicToken()));
        String nombre = ZplTextNormalizer.normalize(q.getNombre());
        String codigo = ZplTextNormalizer.normalize(q.getCodigo());
        String fechaStr = ZplTextNormalizer.normalize(formatDate(q.getFechaEntrada()));
        String caducidadStr = ZplTextNormalizer.normalize(formatDate(q.getCaducidad()));
        String reanalisisStr = ZplTextNormalizer.normalize(
                q.getReanalisis() != null ? formatDate(q.getReanalisis()) : "N/A");
        String documentCode = ZplTextNormalizer.normalize(orEmpty(q.getDocumentCode(), "AL-001-E02/04"));
        String cantidadNorm = ZplTextNormalizer.normalize(cantidadStr);
        String envaseDisplay = ZplTextNormalizer.normalize(
                String.format("%02d", envaseNum) + " de " + String.format("%02d", envaseTotal));
        String envaseTotalStr = ZplTextNormalizer.normalize(String.valueOf(envaseTotal));

        String title = ZplTextNormalizer.normalize("MATERIAL DE ACONDICIONADO");
        String lblFecha = ZplTextNormalizer.normalize("Fecha");
        String lblCodigo = ZplTextNormalizer.normalize("Codigo");
        String lblLote = ZplTextNormalizer.normalize("Lote");
        String lblCaducidad = ZplTextNormalizer.normalize("Caducidad");
        String lblReanalisis = ZplTextNormalizer.normalize("Reanalisis");
        String lblCantidad = ZplTextNormalizer.normalize("Cantidad por envase");
        String lblEnvases = ZplTextNormalizer.normalize("No. de envases");
        String lblCantTotal = ZplTextNormalizer.normalize("Cantidad total");
        String footer = ZplTextNormalizer.normalize(documentCode
                + " Propiedad de Olnatura S.A. de C.V. Prohibido su uso, divulgacion y/o reproduccion total o parcial. "
                + "Si este documento no se encuentra controlado, se considera COPIA SOLO PARA INFORMACION.");

        return "^XA\n" +
                "^PW800\n" +
                "^LL600\n" +
                "^CI13\n" +
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
                "\n" +                "^FO380,185^GB400,300,2^FS\n" +
                "\n" +
                "^FO20,395^GB180,90,2^FS\n" +
                "^FO200,395^GB180,90,2^FS\n" +
                "\n" +
                "^FO20,485^GB760,95,2^FS\n" +
                "\n" +
                "^FO25,25\n^GFA,1080,1080,12,0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001C00000000000000000000000FC000000000000000003FFE07F80000000000000001FFFFC7FE0000000000000007FFFFC3FF800000000000001FFFFFE3FFE00000000000007FFFFFE3FFF8000000000000FFFFFFE1FFFC000000000001FFFFFFF1FFFE000000000003FFFFFFF1FFFF000000000007FFE003F1FFFF80000000000FFF000070FFFF80000000001FFE000018FFFFC0000000003FF8000008FFFFE0000000003FF00000007FFFE0000000007FE00000007FFFE0000000007FC00000003FFFF000000000FFC00000001FFFF000000000FF800000001FFFF000000000FF8000000007FFF000000001FF0000000003FFF000000001FF0000000020FFF000000001FF00000000101FF000000001FF000000001C01C000000001FE000000001F000000000001FE000000001FE00000000001FE000000001FE00000000001FE000000001FE00000000001FE000000001FE00000000001FE000000001FE00000000001FF000000001FE00000000001FF000000001FE00000000001FF000000001FE00000000001FF000000003FE00000000000FF800000003FC00000000000FF800000007FC00000000000FFC00000007FC000000000007FC0000000FFC000000000007FE0000000FF8000000000003FF0000001FF8000000000003FF8000003FF0000000000001FFC000007FF0000000000001FFF00001FFE0000000000000FFF80007FFC00000000000007FFF001FFFC00000000000003FFFFFFFFF800000000000001FFFFFFFFF000000000000000FFFFFFFFE0000000000000007FFFFFFF80000000000000001FFFFFFF000000000000000007FFFFFC000000000000000001FFFFE00000000000000000001FFF000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000^FS\n"
                +
                "\n" +
                "^FO125,36^ADN,18,10" + fdField(title) + "\n" +
                "^FO130,86^ADN,18,10" + fdField(nombre) + "\n" +
                "\n" +
                "^FO28,128^ADN,14,8" + fdField(lblFecha) + "\n" +
                "^FO28,150^ADN,18,10" + fdField(fechaStr) + "\n" +
                "^FO158,128^ADN,14,8" + fdField(lblCodigo) + "\n" +
                "^FO158,150^ADN,18,10" + fdField(codigo) + "\n" +
                "^FO388,128^ADN,14,8" + fdField(lblLote) + "\n" +
                "^FO388,150^ADN,18,10" + fdField(lote) + "\n" +
                "\n" +
                "^FO28,195^ADN,14,8" + fdField(lblCaducidad) + "\n" +
                "^FO28,219^ADN,18,10" + fdField(caducidadStr) + "\n" +
                "^FO28,265^ADN,14,8" + fdField(lblReanalisis) + "\n" +
                "^FO28,289^ADN,18,10" + fdField(reanalisisStr) + "\n" +
                "^FO28,335^ADN,14,8" + fdField(lblCantidad) + "\n" +
                "^FO28,359^ADN,18,10" + fdField(cantidadNorm) + "\n" +
                "\n" +
                qrBlock(qrImageBase64, qrPayload) + "\n" +
                "\n" +
                "^FO28,410^ADN,14,8" + fdField(lblEnvases) + "\n" +
                "^FO28,438^ADN,20,10" + fdField(envaseDisplay) + "\n" +
                "^FO208,410^ADN,14,8" + fdField(lblCantTotal) + "\n" +
                "^FO260,438^ADN,20,10" + fdField(envaseTotalStr) + "\n" +
                "\n" +
                "^FO25,503^ADN,7,4^FB748,4,1,L,0" + fdField(footer) + "\n" +
                "\n" +
                "^XZ\n";
    }

    private QrLabel resolveLabel(String key) {
        if (key.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Identificador vacío");
        }
        QrLabel q;
        try {
            UUID uuid = UUID.fromString(key);
            q = repo.findById(uuid)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada: " + key));
        } catch (IllegalArgumentException ignored) {
            q = repo.findByPublicToken(key)
                    .or(() -> repo.findByLote(key))
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etiqueta no encontrada para: " + key));
        }
        LotOperationalGate.requireActive(q);
        return q;
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
        String payload = ZplTextNormalizer.normalize(qrPayload);
        return "^FO455,190^BQN,2,8\n^FDQA," + payload + "^FS";
    }

    /**
     * Todo texto humano del ZPL pasa por aquí: normaliza ASCII y arma ^FD…^FS.
     * No hay otro camino para escribir texto en la etiqueta.
     */
    private String fdField(String s) {
        String content = ZplTextNormalizer.normalize(s);
        StringBuilder out = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '^' || c == '\\') {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return "^FD" + out + "^FS";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}