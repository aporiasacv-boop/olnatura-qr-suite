package com.company.olnaturaqr.support.audit;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    public static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final int MAX_EXPORT = 5000;

    private final AuditEventRepository repo;
    private final UserRepository userRepository;

    public AuditService(AuditEventRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuditEvent log(
            AuthPrincipal principal,
            String actionType,
            String lote,
            Map<String, Object> metadata,
            String deviceId
    ) {
        return doLog(principal, actionType, lote, metadata, deviceId);
    }

    @Transactional
    public AuditEvent logUnauthenticated(String actionType, String lote, Map<String, Object> metadata, String deviceId) {
        return doLog(null, actionType, lote, metadata, deviceId);
    }

    private AuditEvent doLog(
            AuthPrincipal principal,
            String actionType,
            String lote,
            Map<String, Object> metadata,
            String deviceId
    ) {
        AuditEvent e = new AuditEvent();
        e.setActionType(actionType);
        e.setLote(lote);
        e.setMetadata(metadata);
        e.setDeviceId(deviceId);

        if (principal != null) {
            e.setActorId(principal.id());
            String effectiveRol = null;
            if (metadata != null) {
                Object rol = metadata.get("rol");
                if (rol == null) rol = metadata.get("approvalRole");
                if (rol != null) {
                    String s = String.valueOf(rol).trim();
                    if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                        effectiveRol = s.toUpperCase();
                    }
                }
            }
            if (effectiveRol != null) {
                e.setActorRol(effectiveRol);
            } else {
                e.setActorRol(principal.roles().isEmpty() ? null : principal.roles().get(0));
            }
            userRepository.findById(principal.id()).ifPresent(u -> e.setActorEmail(u.getEmail()));
        }

        return repo.save(e);
    }

    public Page<AuditEvent> list(
            int page,
            int size,
            String actionType,
            String lote,
            String actor,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        PageRequest pr = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAll(AuditEventSpecs.withFilters(
                toStartInstant(fromDate),
                toEndExclusiveInstant(toDate),
                actionType,
                lote,
                actor
        ), pr);
    }

    public List<AuditEvent> listForExport(
            String actionType,
            String lote,
            String actor,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        PageRequest pr = PageRequest.of(0, MAX_EXPORT, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAll(AuditEventSpecs.withFilters(
                toStartInstant(fromDate),
                toEndExclusiveInstant(toDate),
                actionType,
                lote,
                actor
        ), pr).getContent();
    }

    private static Instant toStartInstant(LocalDate d) {
        if (d == null) return null;
        return d.atStartOfDay(ZONE).toInstant();
    }

    private static Instant toEndExclusiveInstant(LocalDate d) {
        if (d == null) return null;
        return d.plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}
