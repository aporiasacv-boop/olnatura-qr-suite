package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditEvent> findByActionTypeOrderByCreatedAtDesc(String actionType, Pageable pageable);

    Page<AuditEvent> findByLoteOrderByCreatedAtDesc(String lote, Pageable pageable);

    List<AuditEvent> findTop500ByLoteOrderByCreatedAtDesc(String lote);
}
