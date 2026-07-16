package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.AdminLotStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/admin/lots")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLotsController {

    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;

    public AdminLotsController(QrLabelRepository qrLabelRepository, AuditService auditService) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<LotAdminDto> list(@RequestParam(required = false) String adminStatus) {
        List<QrLabel> rows;
        if (adminStatus != null && !adminStatus.isBlank()) {
            String st = adminStatus.trim().toUpperCase(Locale.ROOT);
            if (!AdminLotStatus.isValid(st)) {
                throw new ResponseStatusException(BAD_REQUEST, "adminStatus inválido: " + adminStatus);
            }
            rows = qrLabelRepository.findByAdminStatusIgnoreCaseOrderByCreatedAtDesc(st);
        } else {
            rows = qrLabelRepository.findAllByOrderByCreatedAtDesc();
        }
        return rows.stream().map(this::toDto).toList();
    }

    @PatchMapping("/{id}/admin-status")
    public ResponseEntity<LotAdminDto> updateAdminStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminStatusRequest req
    ) {
        if (req == null || req.adminStatus() == null || req.adminStatus().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "adminStatus es requerido");
        }
        String next = req.adminStatus().trim().toUpperCase(Locale.ROOT);
        if (!AdminLotStatus.isValid(next)) {
            throw new ResponseStatusException(BAD_REQUEST, "adminStatus inválido: " + next);
        }

        QrLabel q = qrLabelRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Lote no encontrado"));

        String prev = AdminLotStatus.normalize(q.getAdminStatus());
        q.setAdminStatus(next);
        qrLabelRepository.save(q);

        auditService.log(principal, "CHANGE_LOT_ADMIN_STATUS", q.getLote(),
                Map.of(
                        "labelId", q.getId().toString(),
                        "from", prev,
                        "to", next
                ), null);

        return ResponseEntity.ok(toDto(q));
    }

    private LotAdminDto toDto(QrLabel q) {
        String admin = AdminLotStatus.normalize(q.getAdminStatus());
        return new LotAdminDto(
                q.getId().toString(),
                q.getLote(),
                q.getCodigo(),
                q.getNombre(),
                admin,
                AdminLotStatus.display(admin),
                q.getStatus(),
                q.getCreatedAt() != null ? q.getCreatedAt().toString() : null
        );
    }

    public record LotAdminDto(
            String id,
            String lote,
            String codigo,
            String nombre,
            String adminStatus,
            String adminStatusDisplay,
            String workflowStatus,
            String createdAt
    ) {}

    public record AdminStatusRequest(String adminStatus) {}
}
