package com.company.olnaturaqr.support.audit;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

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
            e.setActorRol(principal.roles().isEmpty() ? null : principal.roles().get(0));
            userRepository.findById(principal.id()).ifPresent(u -> e.setActorEmail(u.getEmail()));
        }

        return repo.save(e);
    }

    public Page<AuditEvent> list(int page, int size, String actionType, String lote) {
        PageRequest pr = PageRequest.of(page, size);
        if (actionType != null && !actionType.isBlank()) {
            return repo.findByActionTypeOrderByCreatedAtDesc(actionType.trim(), pr);
        }
        if (lote != null && !lote.isBlank()) {
            return repo.findByLoteOrderByCreatedAtDesc(lote.trim(), pr);
        }
        return repo.findAllByOrderByCreatedAtDesc(pr);
    }
}
