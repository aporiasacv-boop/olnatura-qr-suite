package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.pdf.AuditPdfService;
import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    /** Acciones que el frontend puede registrar vía POST /log (whitelist). */
    private static final java.util.Set<String> CLIENT_ALLOWED_ACTIONS = java.util.Set.of(
            "GENERATE_LABEL"
    );

    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;
    private final QrLabelRepository qrLabelRepository;
    private final UserRepository userRepository;
    private final AuditPdfService auditPdfService;
    private final ObjectMapper objectMapper;

    public AuditController(
            AuditService auditService,
            AuditEventRepository auditEventRepository,
            QrLabelRepository qrLabelRepository,
            UserRepository userRepository,
            AuditPdfService auditPdfService,
            ObjectMapper objectMapper
    ) {
        this.auditService = auditService;
        this.auditEventRepository = auditEventRepository;
        this.qrLabelRepository = qrLabelRepository;
        this.userRepository = userRepository;
        this.auditPdfService = auditPdfService;
        this.objectMapper = objectMapper;
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
        // Solo eventos de UI permitidos desde el cliente; el resto se audita en servidor.
        String actionType = req.actionType().trim().toUpperCase();
        if (!CLIENT_ALLOWED_ACTIONS.contains(actionType)) {
            return ResponseEntity.badRequest().build();
        }
        AuditEvent e = auditService.log(
                principal,
                actionType,
                req.lote(),
                req.metadata(),
                deviceId
        );
        return ResponseEntity.ok(new AuditLogResponse(e.getId().toString(), "ok"));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @GetMapping("/{lote}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote
    ) {
        String actualLote = resolveToLote(lote);

        List<AuditEvent> events = auditEventRepository.findTop500ByLoteOrderByCreatedAtDesc(actualLote);
        Map<UUID, User> actorsById = loadActors(events);
        Instant generatedAt = Instant.now();

        byte[] pdf;
        try {
            pdf = auditPdfService.generate(actualLote, events, actorsById, generatedAt);
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

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @GetMapping
    public ResponseEntity<Page<AuditEventView>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String lote,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Page<AuditEvent> raw = auditService.list(page, size, actionType, lote, actor, from, to);
        Map<UUID, User> actorsById = loadActors(raw.getContent());
        return ResponseEntity.ok(raw.map(e -> AuditEventView.from(e, actorsById.get(e.getActorId()))));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CALIDAD','INSPECCION')")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String lote,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<AuditEvent> events = auditService.listForExport(actionType, lote, actor, from, to);
        Map<UUID, User> actorsById = loadActors(events);
        String csv = toCsv(events, actorsById);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        Map<String, Object> meta = new HashMap<>();
        meta.put("exportType", "CSV");
        meta.put("countEvents", events.size());
        if (actionType != null) meta.put("actionType", actionType);
        if (lote != null) meta.put("lote", lote);
        if (actor != null) meta.put("actor", actor);
        if (from != null) meta.put("from", from.toString());
        if (to != null) meta.put("to", to.toString());
        meta.put("requester", principal != null ? principal.username() : "anonymous");
        auditService.log(principal, "EXPORT_AUDIT_CSV", lote, meta, null);

        String filename = "auditoria-" + LocalDate.now(AuditService.ZONE) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private Map<UUID, User> loadActors(List<AuditEvent> events) {
        Set<UUID> ids = events.stream()
                .map(AuditEvent::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private String toCsv(List<AuditEvent> events, Map<UUID, User> actorsById) {
        StringBuilder sb = new StringBuilder();
        sb.append("createdAt,accion,usuario,rol,lote,metadata\n");
        for (AuditEvent e : events) {
            AuditEventView view = AuditEventView.from(e, actorsById.get(e.getActorId()));
            sb.append(csv(e.getCreatedAt() != null ? e.getCreatedAt().toString() : "")).append(',');
            sb.append(csv(view.actionTypeDisplay())).append(',');
            sb.append(csv(view.actorDisplay())).append(',');
            sb.append(csv(view.actorRoleDisplay())).append(',');
            sb.append(csv(e.getLote())).append(',');
            sb.append(csv(metadataJson(e.getMetadata()))).append('\n');
        }
        return sb.toString();
    }

    private String metadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return "";
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return String.valueOf(metadata);
        }
    }

    private static String csv(String raw) {
        if (raw == null) return "";
        String s = raw.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s + "\"";
        }
        return s;
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

    public record AuditLogRequest(String actionType, String lote, Map<String, Object> metadata) {}
    public record AuditLogResponse(String id, String status) {}
}
