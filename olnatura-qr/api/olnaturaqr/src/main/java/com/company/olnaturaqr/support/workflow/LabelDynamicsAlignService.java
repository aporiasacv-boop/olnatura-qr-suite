package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.util.SpanishFlexibleDateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class LabelDynamicsAlignService {

    private static final Logger log = LoggerFactory.getLogger(LabelDynamicsAlignService.class);

    private final QrLabelRepository qrLabelRepository;
    private final DynamicsLookupService dynamicsLookupService;
    private final AuditService auditService;
    private final OperationalStatusSyncService operationalStatusSyncService;
    private final TransactionTemplate transactionTemplate;

    public LabelDynamicsAlignService(
            QrLabelRepository qrLabelRepository,
            DynamicsLookupService dynamicsLookupService,
            AuditService auditService,
            OperationalStatusSyncService operationalStatusSyncService,
            PlatformTransactionManager transactionManager
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.dynamicsLookupService = dynamicsLookupService;
        this.auditService = auditService;
        this.operationalStatusSyncService = operationalStatusSyncService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public record AlignRow(
            String labelId,
            String lote,
            String outcome,
            String message,
            List<Map<String, String>> changes,
            boolean reprintRequired
    ) {}

    public record AlignSummary(
            int total,
            int updated,
            int unchanged,
            int notFoundInDynamics,
            int failed,
            List<AlignRow> rows
    ) {}

    public record AlignBatchSummary(
            int total,
            int offset,
            int limit,
            int processed,
            int nextOffset,
            boolean done,
            int updated,
            int unchanged,
            int notFoundInDynamics,
            int failed,
            List<AlignRow> rows
    ) {}

    public AlignSummary alignAll(AuthPrincipal principal) {
        AlignBatchSummary batch = alignBatch(principal, 0, 0);
        return new AlignSummary(
                batch.total(),
                batch.updated(),
                batch.unchanged(),
                batch.notFoundInDynamics(),
                batch.failed(),
                batch.rows()
        );
    }

    public AlignBatchSummary alignBatch(AuthPrincipal principal, int offset, int limit) {
        List<QrLabel> labels = qrLabelRepository.findAllByOrderByCreatedAtDesc();
        int total = labels.size();
        int safeOffset = Math.max(0, offset);
        int safeLimit;
        if (limit <= 0) {
            safeLimit = Math.max(0, total - safeOffset);
        } else {
            safeLimit = Math.min(limit, 50);
        }

        int from = Math.min(safeOffset, total);
        int to = Math.min(from + safeLimit, total);
        List<QrLabel> slice = labels.subList(from, to);

        List<AlignRow> rows = new ArrayList<>();
        int updated = 0;
        int unchanged = 0;
        int notFound = 0;
        int failed = 0;

        for (QrLabel snapshot : slice) {
            UUID id = snapshot.getId();
            String lote = snapshot.getLote();
            try {
                AlignRow row = transactionTemplate.execute(status -> {
                    QrLabel label = qrLabelRepository.findById(id).orElse(null);
                    if (label == null) {
                        return new AlignRow(id.toString(), lote, "FAILED", "Etiqueta no encontrada", List.of(), false);
                    }
                    return alignLabel(label, principal);
                });
                if (row == null) {
                    failed++;
                    rows.add(new AlignRow(id.toString(), lote, "FAILED", "Transacción sin resultado", List.of(), false));
                    continue;
                }
                rows.add(row);
                switch (row.outcome()) {
                    case "UPDATED" -> updated++;
                    case "UNCHANGED" -> unchanged++;
                    case "NOT_FOUND" -> notFound++;
                    default -> failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("AlignDynamics falló lote={} err={}", lote, e.toString());
                rows.add(new AlignRow(
                        id != null ? id.toString() : null,
                        lote,
                        "FAILED",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                        List.of(),
                        false
                ));
            }
        }

        int processed = slice.size();
        int nextOffset = from + processed;
        boolean done = nextOffset >= total;

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("total", total);
        md.put("offset", from);
        md.put("limit", safeLimit);
        md.put("processed", processed);
        md.put("nextOffset", nextOffset);
        md.put("done", done);
        md.put("updated", updated);
        md.put("unchanged", unchanged);
        md.put("notFoundInDynamics", notFound);
        md.put("failed", failed);
        auditService.log(principal, "ADMIN_ALIGN_LABELS_DYNAMICS_BATCH", null, md, null);

        return new AlignBatchSummary(
                total,
                from,
                safeLimit,
                processed,
                nextOffset,
                done,
                updated,
                unchanged,
                notFound,
                failed,
                rows
        );
    }

    public AlignRow alignOne(AuthPrincipal principal, UUID labelId) {
        QrLabel snapshot = qrLabelRepository.findById(labelId).orElse(null);
        if (snapshot == null) {
            return new AlignRow(
                    labelId != null ? labelId.toString() : null,
                    null,
                    "FAILED",
                    "Etiqueta no encontrada",
                    List.of(),
                    false
            );
        }
        String lote = snapshot.getLote();
        try {
            AlignRow row = transactionTemplate.execute(status -> {
                QrLabel label = qrLabelRepository.findById(labelId).orElse(null);
                if (label == null) {
                    return new AlignRow(labelId.toString(), lote, "FAILED", "Etiqueta no encontrada", List.of(), false);
                }
                return alignLabel(label, principal);
            });
            if (row == null) {
                return new AlignRow(labelId.toString(), lote, "FAILED", "Transacción sin resultado", List.of(), false);
            }
            return row;
        } catch (Exception e) {
            log.warn("AlignDynamics one falló lote={} err={}", lote, e.toString());
            return new AlignRow(
                    labelId.toString(),
                    lote,
                    "FAILED",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    List.of(),
                    false
            );
        }
    }

    public AlignRow confirmReprint(AuthPrincipal principal, UUID labelId) {
        QrLabel label = qrLabelRepository.findById(labelId).orElse(null);
        if (label == null) {
            return new AlignRow(
                    labelId != null ? labelId.toString() : null,
                    null,
                    "FAILED",
                    "Etiqueta no encontrada",
                    List.of(),
                    false
            );
        }
        boolean wasRequired = label.isReprintRequired();
        LabelReprintGate.clear(label);
        QrLabel saved = qrLabelRepository.save(label);

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", saved.getId().toString());
        md.put("wasRequired", wasRequired);
        auditService.log(principal, "ADMIN_CONFIRM_LABEL_REPRINT", saved.getLote(), md, null);

        return new AlignRow(
                saved.getId().toString(),
                saved.getLote(),
                "CONFIRMED",
                wasRequired
                        ? "Reimpresión confirmada: la etiqueta física se considera actualizada"
                        : "Sin pendiente de reimpresión",
                List.of(),
                false
        );
    }

    private AlignRow alignLabel(QrLabel label, AuthPrincipal principal) {
        Optional<DynamicsLookupDto> dynOpt = dynamicsLookupService.lookupByBatchNumber(label.getLote());
        if (dynOpt.isEmpty()) {
            return new AlignRow(
                    label.getId().toString(),
                    label.getLote(),
                    "NOT_FOUND",
                    "Sin datos en Dynamics para este lote",
                    List.of(),
                    label.isReprintRequired()
            );
        }

        DynamicsLookupDto d = dynOpt.get();
        List<Map<String, String>> changes = new ArrayList<>();

        String nextNombre = blankToNull(d.nombre());
        if (nextNombre != null && !sameText(nextNombre, label.getNombre())) {
            changes.add(change("nombre", "Nombre", nullToDash(label.getNombre()), nextNombre));
            label.setNombre(nextNombre);
        }

        String nextCodigo = blankToNull(d.codigo());
        if (nextCodigo != null && !sameText(nextCodigo, label.getCodigo())) {
            changes.add(change("codigo", "Código", nullToDash(label.getCodigo()), nextCodigo));
            label.setCodigo(nextCodigo);
        }

        LocalDate nextFechaEntrada = SpanishFlexibleDateParser.parseOptional(d.fechaEntrada());
        if (nextFechaEntrada != null && !Objects.equals(label.getFechaEntrada(), nextFechaEntrada)) {
            changes.add(change(
                    "fechaEntrada",
                    "Fecha de entrada",
                    dateStr(label.getFechaEntrada()),
                    dateStr(nextFechaEntrada)
            ));
            label.setFechaEntrada(nextFechaEntrada);
        }

        LocalDate nextCaducidad = SpanishFlexibleDateParser.parseOptional(d.caducidad());
        if (d.caducidad() != null && !d.caducidad().isBlank()
                && !Objects.equals(label.getCaducidad(), nextCaducidad)) {
            changes.add(change(
                    "caducidad",
                    "Caducidad",
                    dateStr(label.getCaducidad()),
                    dateStr(nextCaducidad)
            ));
            label.setCaducidad(nextCaducidad);
        }

        OperationalStatusSyncService.SyncResult statusSync =
                operationalStatusSyncService.applyDynamicsStatus(
                        label,
                        d.operationalStatus(),
                        OperationalStatusSyncService.REASON_ALIGN,
                        principal
                );
        if (statusSync.updated() && statusSync.to() != null) {
            changes.add(change(
                    "status",
                    "Estado operativo (Dynamics)",
                    statusSync.from(),
                    statusSync.to()
            ));
        }

        boolean clearedReprint = false;
        if (label.isReprintRequired()) {
            LabelReprintGate.clear(label);
            clearedReprint = true;
        }

        if (changes.isEmpty() && !clearedReprint) {
            return new AlignRow(
                    label.getId().toString(),
                    label.getLote(),
                    "UNCHANGED",
                    "Ya coincide con Dynamics (envases/QR intactos)",
                    List.of(),
                    false
            );
        }

        QrLabel saved = qrLabelRepository.save(label);

        if (changes.isEmpty()) {
            Map<String, Object> mdClear = new LinkedHashMap<>();
            mdClear.put("labelId", saved.getId().toString());
            mdClear.put("clearedReprintOnly", true);
            auditService.log(principal, "ADMIN_ALIGN_LABEL_DYNAMICS", saved.getLote(), mdClear, null);
            return new AlignRow(
                    saved.getId().toString(),
                    saved.getLote(),
                    "UNCHANGED",
                    "Ya coincide con Dynamics",
                    List.of(),
                    false
            );
        }

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", saved.getId().toString());
        md.put("preserved", List.of("publicToken", "envaseNum", "envaseTotal", "cantidadPorEnvase", "restosEnabled", "cantidadResto", "restosCantidades"));
        md.put("operationalStatus", d.operationalStatus());
        md.put("changes", changes);
        md.put("reprintRequired", false);
        md.put("clearedReprint", clearedReprint);
        auditService.log(principal, "ADMIN_ALIGN_LABEL_DYNAMICS", saved.getLote(), md, null);

        return new AlignRow(
                saved.getId().toString(),
                saved.getLote(),
                "UPDATED",
                "Actualizado con datos actuales de Dynamics (envases, cantidad y QR preservados)",
                changes,
                false
        );
    }

    private static Map<String, String> change(String field, String fieldLabel, String from, String to) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("fieldLabel", fieldLabel);
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private static boolean sameText(String a, String b) {
        return normalizeText(a).equalsIgnoreCase(normalizeText(b));
    }

    private static String normalizeText(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String dateStr(LocalDate d) {
        return d == null ? "—" : d.toString();
    }
}
