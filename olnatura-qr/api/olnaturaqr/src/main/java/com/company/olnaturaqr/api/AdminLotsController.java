package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.AdminLabelCorrectionService;
import com.company.olnaturaqr.support.workflow.AdminLotStatus;
import com.company.olnaturaqr.support.workflow.AdminStatusCorrectionService;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.company.olnaturaqr.support.qr.LoteExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/admin/lots")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLotsController {

    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;
    private final AdminLabelCorrectionService correctionService;
    private final AdminStatusCorrectionService statusCorrectionService;

    public AdminLotsController(
            QrLabelRepository qrLabelRepository,
            AuditService auditService,
            AdminLabelCorrectionService correctionService,
            AdminStatusCorrectionService statusCorrectionService
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
        this.correctionService = correctionService;
        this.statusCorrectionService = statusCorrectionService;
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

    /**
     * Corrección administrativa de datos de captura (etiqueta).
     * Motivo obligatorio. Cada campo modificado queda en auditoría.
     */
    @PatchMapping("/by-lote/{lote}/correct")
    public ResponseEntity<CorrectionResponse> correctByLote(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote,
            @RequestBody AdminLabelCorrectionService.CorrectionRequest req
    ) {
        QrLabel q = resolveLabel(lote);
        var result = correctionService.correct(q, principal, req);
        return ResponseEntity.ok(toCorrectionResponse(result));
    }

    @PatchMapping("/{id}/correct")
    public ResponseEntity<CorrectionResponse> correctById(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminLabelCorrectionService.CorrectionRequest req
    ) {
        QrLabel q = qrLabelRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Lote no encontrado"));
        LotOperationalGate.requireActive(q);
        var result = correctionService.correct(q, principal, req);
        return ResponseEntity.ok(toCorrectionResponse(result));
    }

    /**
     * Corrección administrativa excepcional del <strong>estado de plataforma</strong>
     * ({@code qr_labels.status}). No es una aprobación.
     * Solo cambia platformStatus; no altera historial de aprobaciones ni comentarios
     * ni el Estado Operativo Dynamics.
     */
    @PatchMapping("/by-lote/{lote}/correct-status")
    public ResponseEntity<StatusCorrectionResponse> correctStatusByLote(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String lote,
            @RequestBody AdminStatusCorrectionService.StatusCorrectionRequest req
    ) {
        QrLabel q = resolveLabel(lote);
        var result = statusCorrectionService.correct(q, principal, req);
        return ResponseEntity.ok(toStatusCorrectionResponse(result));
    }

    @PatchMapping("/{id}/correct-status")
    public ResponseEntity<StatusCorrectionResponse> correctStatusById(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminStatusCorrectionService.StatusCorrectionRequest req
    ) {
        QrLabel q = qrLabelRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Lote no encontrado"));
        LotOperationalGate.requireActive(q);
        var result = statusCorrectionService.correct(q, principal, req);
        return ResponseEntity.ok(toStatusCorrectionResponse(result));
    }

    private QrLabel resolveLabel(String raw) {
        String identifier = LoteExtractor.extract(raw).orElse(raw != null ? raw.trim() : "");
        if (identifier.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Identificador vacío");
        }
        QrLabel label = qrLabelRepository.findByPublicToken(identifier)
                .or(() -> qrLabelRepository.findByLote(identifier))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + identifier));
        LotOperationalGate.requireActive(label);
        return label;
    }

    private CorrectionResponse toCorrectionResponse(AdminLabelCorrectionService.CorrectionResult result) {
        QrLabel q = result.label();
        List<Map<String, String>> changes = result.changes().stream().map(c -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("field", c.field());
            m.put("fieldLabel", c.fieldLabel());
            m.put("from", c.from());
            m.put("to", c.to());
            return m;
        }).collect(Collectors.toList());
        return new CorrectionResponse(
                q.getId().toString(),
                q.getLote(),
                LabelDto.LabelView.from(q),
                changes
        );
    }

    private StatusCorrectionResponse toStatusCorrectionResponse(
            AdminStatusCorrectionService.StatusCorrectionResult result
    ) {
        QrLabel q = result.label();
        return new StatusCorrectionResponse(
                q.getId().toString(),
                q.getLote(),
                WorkflowStatus.normalize(q.getStatus()),
                result.from(),
                result.to(),
                result.motivo(),
                AdminStatusCorrectionService.allowedTargets(q.getStatus())
        );
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
            /** Workflow de plataforma ({@code qr_labels.status}); no es Estado Operativo Dynamics. */
            String workflowStatus,
            String createdAt
    ) {}

    public record AdminStatusRequest(String adminStatus) {}

    public record CorrectionResponse(
            String id,
            String lote,
            LabelDto.LabelView label,
            List<Map<String, String>> changes
    ) {}

    public record StatusCorrectionResponse(
            String id,
            String lote,
            /** platformStatus resultante ({@code qr_labels.status}). */
            String status,
            String from,
            String to,
            String motivo,
            List<String> allowedNext
    ) {}
}
