package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.AdminLabelCorrectionService;
import com.company.olnaturaqr.support.workflow.AdminLotDeleteService;
import com.company.olnaturaqr.support.workflow.AdminLotStatus;
import com.company.olnaturaqr.support.workflow.LabelDynamicsAlignService;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
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
    private final LabelDynamicsAlignService labelDynamicsAlignService;
    private final AdminLotDeleteService lotDeleteService;

    public AdminLotsController(
            QrLabelRepository qrLabelRepository,
            AuditService auditService,
            AdminLabelCorrectionService correctionService,
            LabelDynamicsAlignService labelDynamicsAlignService,
            AdminLotDeleteService lotDeleteService
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
        this.correctionService = correctionService;
        this.labelDynamicsAlignService = labelDynamicsAlignService;
        this.lotDeleteService = lotDeleteService;
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

    @PostMapping("/align-from-dynamics")
    public LabelDynamicsAlignService.AlignSummary alignFromDynamics(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return labelDynamicsAlignService.alignAll(principal);
    }

    @PostMapping("/sync-dynamics-batch")
    public LabelDynamicsAlignService.AlignBatchSummary syncDynamicsBatch(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        int safeOffset = Math.max(offset, 0);
        return labelDynamicsAlignService.alignBatch(principal, safeOffset, safeLimit);
    }

    @PostMapping("/{id}/sync-dynamics")
    public LabelDynamicsAlignService.AlignRow syncDynamicsOne(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id
    ) {
        return labelDynamicsAlignService.alignOne(principal, id);
    }

    @PostMapping("/{id}/confirm-reprint")
    public LabelDynamicsAlignService.AlignRow confirmReprint(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id
    ) {
        return labelDynamicsAlignService.confirmReprint(principal, id);
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

    @PostMapping("/{id}/delete")
    public ResponseEntity<AdminLotDeleteService.DeleteResult> deleteLot(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminLotDeleteService.DeleteRequest req
    ) {
        return ResponseEntity.ok(lotDeleteService.deleteLot(id, principal, req));
    }

    
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
                q.getCreatedAt() != null ? q.getCreatedAt().toString() : null,
                q.isReprintRequired(),
                q.getReprintRequiredAt() != null ? q.getReprintRequiredAt().toString() : null
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
            String createdAt,
            boolean reprintRequired,
            String reprintRequiredAt
    ) {}

    public record AdminStatusRequest(String adminStatus) {}

    public record CorrectionResponse(
            String id,
            String lote,
            LabelDto.LabelView label,
            List<Map<String, String>> changes
    ) {}
}
