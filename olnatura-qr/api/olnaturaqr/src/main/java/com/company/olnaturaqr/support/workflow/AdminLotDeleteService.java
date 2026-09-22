package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.LoteCommentRepository;
import com.company.olnaturaqr.repository.ProblemReportRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminLotDeleteService {

    public static final String CONFIRM_TOKEN = "ELIMINAR_LOTE";
    public static final String AUDIT_ACTION = "ELIMINAR_LOTE";

    private final QrLabelRepository qrLabelRepository;
    private final LoteCommentRepository loteCommentRepository;
    private final ScanEventRepository scanEventRepository;
    private final ProblemReportRepository problemReportRepository;
    private final AuditService auditService;

    public AdminLotDeleteService(
            QrLabelRepository qrLabelRepository,
            LoteCommentRepository loteCommentRepository,
            ScanEventRepository scanEventRepository,
            ProblemReportRepository problemReportRepository,
            AuditService auditService
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.loteCommentRepository = loteCommentRepository;
        this.scanEventRepository = scanEventRepository;
        this.problemReportRepository = problemReportRepository;
        this.auditService = auditService;
    }

    public record DeleteRequest(String confirm, String lote) {}

    public record DeleteResult(String id, String lote) {}

    @Transactional
    public DeleteResult deleteLot(UUID id, AuthPrincipal principal, DeleteRequest req) {
        if (req == null || req.confirm() == null || !CONFIRM_TOKEN.equals(req.confirm().trim())) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requiere confirm=\"" + CONFIRM_TOKEN + "\"");
        }
        String loteConfirm = req.lote() == null ? "" : req.lote().trim();
        if (loteConfirm.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "lote es requerido para confirmar la eliminación");
        }

        QrLabel q = qrLabelRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Lote no encontrado"));
        String lote = q.getLote();
        if (!loteConfirm.equals(lote)) {
            throw new ResponseStatusException(BAD_REQUEST, "El lote de confirmación no coincide");
        }

        String labelId = q.getId().toString();
        String publicToken = q.getPublicToken();

        loteCommentRepository.deleteByLote(lote);
        scanEventRepository.deleteByLote(lote);
        problemReportRepository.deleteByLote(lote);
        qrLabelRepository.delete(q);
        qrLabelRepository.flush();

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", labelId);
        md.put("lote", lote);
        md.put("publicToken", publicToken);
        auditService.log(principal, AUDIT_ACTION, lote, md, null);

        return new DeleteResult(labelId, lote);
    }
}
