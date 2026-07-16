package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.scan.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScanEventRepository extends JpaRepository<ScanEvent, UUID> {
    List<ScanEvent> findTop50ByLoteOrderByCreatedAtDesc(String lote);

    @Query(value = """
            SELECT COUNT(*) FROM scan_events
            WHERE created_at >= :from AND created_at < :to
            """, nativeQuery = true)
    long countByCreatedAtInstantBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT CAST((created_at AT TIME ZONE 'America/Mexico_City') AS date) AS day,
                   COUNT(*) AS total
            FROM scan_events
            WHERE created_at >= :from AND created_at < :to
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countScansGroupedByDay(@Param("from") Instant from, @Param("to") Instant to);
}