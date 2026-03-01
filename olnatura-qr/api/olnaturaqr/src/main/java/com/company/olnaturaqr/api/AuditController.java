package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
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
