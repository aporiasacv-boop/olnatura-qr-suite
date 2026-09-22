package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Sincroniza {@code qr_labels.status} (copia local del Estado Operativo) desde Dynamics.
 * Ante cualquier diferencia, prevalece el valor calculado desde Dynamics 365 F&amp;O.
 * No modifica {@code admin_status}.
 */
@Service
public class OperationalStatusSyncService {

    private static final Logger log = LoggerFactory.getLogger(OperationalStatusSyncService.class);

    public static final String REASON_CONSULTA = "CONSULTA_LOTE";
    public static final String REASON_SYNC_MANUAL = "SYNC_DYNAMICS_MANUAL";
    public static final String REASON_ALIGN = "ALIGN_FROM_DYNAMICS";
    public static final String REASON_LABEL_CREATE = "GENERACION_ETIQUETA";
    public static final String REASON_BACKFILL = "BACKFILL_STARTUP";

    public static final String AUDIT_ACTION = "SYNC_OPERATIONAL_STATUS_DYNAMICS";

    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;

    public OperationalStatusSyncService(QrLabelRepository qrLabelRepository, AuditService auditService) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
    }

    public record SyncResult(
            boolean updated,
            String lote,
            String from,
            String to,
            String reason,
            Instant at
    ) {}

    /**
     * Persiste el estado operativo de Dynamics en la etiqueta si difiere.
     * Estados definitivos: APROBADO, CUARENTENA, RECHAZADO.
     * {@code DESCONOCIDO}/vacío no sobrescribe (información insuficiente).
     * PARCIAL legado se trata como APROBADO.
     */
    @Transactional
    public SyncResult applyDynamicsStatus(
            QrLabel label,
            String dynamicsOperationalStatus,
            String reason,
            AuthPrincipal principal
    ) {
        Objects.requireNonNull(label, "label");
        String to = canonicalizeForStorage(dynamicsOperationalStatus);
        if (to == null) {
            return new SyncResult(
                    false,
                    label.getLote(),
                    normalizeStored(label.getStatus()),
                    null,
                    reason,
                    Instant.now()
            );
        }

        String from = normalizeStored(label.getStatus());
        if (from.equals(to)) {
            return new SyncResult(false, label.getLote(), from, to, reason, Instant.now());
        }

        label.setStatus(to);
        QrLabel saved = qrLabelRepository.save(label);
        Instant at = Instant.now();

        log.info(
                "[EstadoOperativoSync] lote={} from={} to={} reason={} at={}",
                saved.getLote(),
                from,
                to,
                reason,
                at
        );

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", saved.getId().toString());
        md.put("lote", saved.getLote());
        md.put("from", from);
        md.put("to", to);
        md.put("estadoAnterior", from);
        md.put("estadoNuevo", to);
        md.put("fecha", at.toString());
        md.put("motivo", reason);
        md.put("motivoActualizacion", reason);
        md.put("source", OperationalStatusResolver.SOURCE_DYNAMICS);
        auditService.log(principal, AUDIT_ACTION, saved.getLote(), md, null);

        return new SyncResult(true, saved.getLote(), from, to, reason, at);
    }

    /**
     * Variante aislada (p. ej. consulta read-only outer) que recarga la etiqueta por id.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult applyDynamicsStatusById(
            UUID labelId,
            String dynamicsOperationalStatus,
            String reason,
            AuthPrincipal principal
    ) {
        QrLabel label = qrLabelRepository.findById(labelId).orElse(null);
        if (label == null) {
            return new SyncResult(false, null, null, null, reason, Instant.now());
        }
        return applyDynamicsStatus(label, dynamicsOperationalStatus, reason, principal);
    }

    /**
     * Normaliza el valor almacenado en {@code qr_labels.status} para comparación/UI.
     * PARCIAL legado → APROBADO.
     */
    public static String normalizeStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return OperationalStatusResolver.STATUS_CUARENTENA;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (OperationalStatusResolver.STATUS_PARCIAL.equals(s)) {
            return OperationalStatusResolver.STATUS_APROBADO;
        }
        if (OperationalStatusResolver.STATUS_APROBADO.equals(s)
                || OperationalStatusResolver.STATUS_RECHAZADO.equals(s)
                || OperationalStatusResolver.STATUS_CUARENTENA.equals(s)) {
            return s;
        }
        return OperationalStatusResolver.STATUS_CUARENTENA;
    }

    /**
     * @return estado a persistir, o {@code null} si Dynamics no aporta un valor definitivo
     */
    public static String canonicalizeForStorage(String dynamicsOperationalStatus) {
        if (dynamicsOperationalStatus == null || dynamicsOperationalStatus.isBlank()) {
            return null;
        }
        String s = dynamicsOperationalStatus.trim().toUpperCase(Locale.ROOT);
        if (OperationalStatusResolver.STATUS_DESCONOCIDO.equals(s)) {
            return null;
        }
        if (OperationalStatusResolver.STATUS_PARCIAL.equals(s)) {
            return OperationalStatusResolver.STATUS_APROBADO;
        }
        if (OperationalStatusResolver.STATUS_APROBADO.equals(s)
                || OperationalStatusResolver.STATUS_RECHAZADO.equals(s)
                || OperationalStatusResolver.STATUS_CUARENTENA.equals(s)) {
            return s;
        }
        return null;
    }
}
