package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditEvent> findByActionTypeOrderByCreatedAtDesc(String actionType, Pageable pageable);

    Page<AuditEvent> findByLoteOrderByCreatedAtDesc(String lote, Pageable pageable);

    List<AuditEvent> findTop500ByLoteOrderByCreatedAtDesc(String lote);

    List<AuditEvent> findByLoteAndActionTypeInOrderByCreatedAtAsc(String lote, Collection<String> actionTypes);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    AuditEvent findFirstByActionTypeOrderByCreatedAtDesc(String actionType);
}
