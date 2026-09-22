package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.report.ProblemReport;
import com.company.olnaturaqr.repository.ProblemReportRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/admin/problem-reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProblemReportsController {

    private final ProblemReportRepository repository;

    public AdminProblemReportsController(ProblemReportRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProblemReportController.ProblemReportDto> list(
            @RequestParam(value = "status", required = false) String status
    ) {
        List<ProblemReport> rows;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            rows = repository.findTop100ByOrderByCreatedAtDesc();
        } else {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!"OPEN".equals(normalized) && !"RESOLVED".equals(normalized)) {
                throw new ResponseStatusException(BAD_REQUEST, "Estado inválido");
            }
            rows = repository.findTop100ByStatusOrderByCreatedAtDesc(normalized);
        }
        return rows.stream().map(this::toDto).toList();
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ProblemReportController.ProblemReportDto> resolve(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ProblemReport report = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Reporte no encontrado"));

        if (!"RESOLVED".equals(report.getStatus())) {
            report.setStatus("RESOLVED");
            report.setResolvedAt(OffsetDateTime.now());
            if (principal != null && principal.username() != null && !principal.username().isBlank()) {
                report.setResolvedByUsername(principal.username().trim());
            }
            report = repository.save(report);
        }

        return ResponseEntity.ok(toDto(report));
    }

    private ProblemReportController.ProblemReportDto toDto(ProblemReport r) {
        return new ProblemReportController.ProblemReportDto(
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
}
