package com.company.olnaturaqr.api.temp;

import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.LoteCommentRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/temp/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationalPurgeTempController {

    public static final String CONFIRM_TOKEN = "PURGE_LOTS";
    public static final String AUDIT_ACTION = "PURGE_LOTS";

    private final AdminOperationalPurgeTempService purgeService;

    public AdminOperationalPurgeTempController(AdminOperationalPurgeTempService purgeService) {
        this.purgeService = purgeService;
    }

    @PostMapping("/purge-operational-data")
    public PurgeResponse purge(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody(required = false) PurgeRequest req
    ) {
        String confirm = req == null || req.confirm() == null ? "" : req.confirm().trim();
        if (!CONFIRM_TOKEN.equals(confirm)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Se requiere confirm=\"" + CONFIRM_TOKEN + "\""
            );
        }
        return purgeService.purgeLots(principal);
    }

    public record PurgeRequest(String confirm) {}

    public record PurgeResponse(
            long loteCommentsDeleted,
            long scanEventsDeleted,
            long auditEventsDeleted,
            long qrLabelsDeleted
    ) {}
}

@Service
class AdminOperationalPurgeTempService {

    private final LoteCommentRepository loteCommentRepository;
    private final ScanEventRepository scanEventRepository;
    private final AuditEventRepository auditEventRepository;
    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;

    AdminOperationalPurgeTempService(
            LoteCommentRepository loteCommentRepository,
            ScanEventRepository scanEventRepository,
            AuditEventRepository auditEventRepository,
            QrLabelRepository qrLabelRepository,
            AuditService auditService
    ) {
        this.loteCommentRepository = loteCommentRepository;
        this.scanEventRepository = scanEventRepository;
        this.auditEventRepository = auditEventRepository;
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AdminOperationalPurgeTempController.PurgeResponse purgeLots(AuthPrincipal principal) {
        long comments = loteCommentRepository.count();
        loteCommentRepository.deleteAllInBatch();

        long scans = scanEventRepository.count();
        scanEventRepository.deleteAllInBatch();

        long audits = auditEventRepository.countByLoteIsNotNull();
        auditEventRepository.deleteByLoteIsNotNull();

        long labels = qrLabelRepository.count();
        qrLabelRepository.deleteAllInBatch();

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("loteCommentsDeleted", comments);
        md.put("scanEventsDeleted", scans);
        md.put("auditEventsDeleted", audits);
        md.put("qrLabelsDeleted", labels);
        auditService.log(principal, AdminOperationalPurgeTempController.AUDIT_ACTION, null, md, null);

        return new AdminOperationalPurgeTempController.PurgeResponse(
                comments, scans, audits, labels
        );
    }
}
