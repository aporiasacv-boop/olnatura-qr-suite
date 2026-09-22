package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.report.ProblemReport;
import com.company.olnaturaqr.repository.ProblemReportRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/reports")
public class ProblemReportController {

    private static final Set<String> KINDS = Set.of("SCAN", "ACCESS");
    private static final int MAX_REASON = 200;
    private static final int MAX_COMMENT = 1000;

    private final ProblemReportRepository repository;

    public ProblemReportController(ProblemReportRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<ProblemReportDto> create(
            @RequestBody CreateProblemReportRequest req,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (req == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Solicitud inválida");
        }

        String kind = req.kind() == null ? "" : req.kind().trim().toUpperCase(Locale.ROOT);
        if (!KINDS.contains(kind)) {
            throw new ResponseStatusException(BAD_REQUEST, "Tipo de reporte inválido");
        }

        String reason = req.reason() == null ? "" : req.reason().trim();
        if (reason.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Selecciona un motivo");
        }
        if (reason.length() > MAX_REASON) {
            throw new ResponseStatusException(BAD_REQUEST, "El motivo es demasiado largo");
        }

        String comment = req.comment() == null ? "" : req.comment().trim();
        if (comment.length() > MAX_COMMENT) {
            throw new ResponseStatusException(BAD_REQUEST, "El comentario supera el máximo de " + MAX_COMMENT + " caracteres");
        }

        String lote = req.lote() == null ? null : req.lote().trim();
        if (lote != null && lote.isEmpty()) {
            lote = null;
        }
        if ("SCAN".equals(kind) && (lote == null || "ACCESO".equalsIgnoreCase(lote))) {
            lote = null;
        }
        if ("ACCESS".equals(kind)) {
            lote = null;
        }

        ProblemReport report = new ProblemReport();
        report.setKind(kind);
        report.setLote(lote);
        report.setReason(reason);
        report.setCommentBody(comment.isEmpty() ? null : comment);
        report.setStatus("OPEN");

        if (principal != null && principal.id() != null) {
            report.setReporterUserId(principal.id());
            String username = principal.username();
            if (username != null && !username.isBlank()) {
                report.setReporterUsername(username.trim());
            }
        }

        ProblemReport saved = repository.save(report);
        return ResponseEntity.ok(toDto(saved));
    }

    private static ProblemReportDto toDto(ProblemReport r) {
        return new ProblemReportDto(
                r.getId() == null ? null : r.getId().toString(),
                r.getKind(),
                r.getLote(),
                r.getReason(),
                r.getCommentBody(),
                r.getReporterUsername(),
                r.getStatus(),
                r.getCreatedAt() == null ? null : r.getCreatedAt().toString(),
                r.getResolvedAt() == null ? null : r.getResolvedAt().toString(),
                r.getResolvedByUsername()
        );
    }

    public record CreateProblemReportRequest(String kind, String lote, String reason, String comment) {}

    public record ProblemReportDto(
            String id,
            String kind,
            String lote,
            String reason,
            String comment,
            String reporterUsername,
            String status,
            String createdAt,
            String resolvedAt,
            String resolvedByUsername
    ) {}
}
