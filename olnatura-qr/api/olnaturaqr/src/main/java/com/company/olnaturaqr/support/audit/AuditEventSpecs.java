package com.company.olnaturaqr.support.audit;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditEventSpecs {

    private AuditEventSpecs() {}

    public static Specification<AuditEvent> withFilters(
            Instant fromInclusive,
            Instant toExclusive,
            String actionType,
            String lote,
            String actor
    ) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (fromInclusive != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
            }
            if (toExclusive != null) {
                preds.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            if (actionType != null && !actionType.isBlank()) {
                preds.add(cb.equal(cb.upper(root.get("actionType")), actionType.trim().toUpperCase()));
            }
            if (lote != null && !lote.isBlank()) {
                preds.add(cb.like(cb.upper(root.get("lote")), "%" + lote.trim().toUpperCase() + "%"));
            }
            if (actor != null && !actor.isBlank()) {
                String pattern = "%" + actor.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("actorEmail")), pattern),
                        cb.like(cb.lower(root.get("actorRol")), pattern)
                ));
            }

            if (query != null && query.getOrderList().isEmpty()) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
