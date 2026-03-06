package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.pdf.AuditPdfService;
import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;
    private final QrLabelRepository qrLabelRepository;
    private final AuditPdfService auditPdfService;

    public AuditController(AuditService auditService, AuditEventRepository auditEventRepository,
                           QrLabelRepository qrLabelRepository, AuditPdfService auditPdfService) {
        this.auditService = auditService;
        this.auditEventRepository = auditEventRepository;
        this.qrLabelRepository = qrLabelRepository;
        this.auditPdfService = auditPdfService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/log")
    public ResponseEntity<AuditLogResponse> log(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody AuditLogRequest req,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId
    ) {
        if (req == null || req.actionType() == null || req.actionType().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuditEvent e = auditService.log(
                principal,
                req.actionType().trim(),
                req.lote(),
                req.metadata(),
                deviceId
        );
        return ResponseEntity.ok(new AuditLogResponse(e.getId().toString(), "ok"));
    }

    @PreAuthorize("hasAnyRole('ADMIN','INSPECCION')")
    @GetMapping("/{lote}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote
    ) {
        String actualLote = resolveToLote(lote);

        List<AuditEvent> events = auditEventRepository.findTop500ByLoteOrderByCreatedAtDesc(actualLote);
        Instant generatedAt = Instant.now();

        byte[] pdf;
        try {
            pdf = auditPdfService.generate(actualLote, events, generatedAt);
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF", e);
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("lote", actualLote);
        meta.put("exportType", "PDF");
        meta.put("countEvents", events.size());
        meta.put("requester", principal != null ? principal.username() : "anonymous");
        auditService.log(principal, "EXPORT_AUDIT_PDF", actualLote, meta, null);

        String filename = "trazabilidad-" + sanitizeFilename(actualLote) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private String resolveToLote(String raw) {
        String identifier = LoteExtractor.extract(raw).orElse(raw != null ? raw.trim() : "");
        if (identifier.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Identificador vacío");
        }
        return qrLabelRepository.findByPublicToken(identifier)
                .or(() -> qrLabelRepository.findByLote(identifier))
                .map(QrLabel::getLote)
                .orElse(identifier);
    }

    private static String sanitizeFilename(String s) {
        if (s == null) return "lote";
        return s.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AuditEvent>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String lote
    ) {
        return ResponseEntity.ok(auditService.list(page, size, actionType, lote));
    }

    public record AuditLogRequest(String actionType, String lote, Map<String, Object> metadata) {}
    public record AuditLogResponse(String id, String status) {}
}
