package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.qr.QrLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrLabelRepository extends JpaRepository<QrLabel, UUID> {
    Optional<QrLabel> findByLote(String lote);
    Optional<QrLabel> findByPublicToken(String publicToken);
    boolean existsByLote(String lote);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    long countByAdminStatusIgnoreCase(String adminStatus);

    List<QrLabel> findAllByOrderByCreatedAtDesc();

    List<QrLabel> findByAdminStatusIgnoreCaseOrderByCreatedAtDesc(String adminStatus);

    /**
     * Autocompletado de lotes: prefijo y coincidencia parcial sobre lote/código/nombre.
     * Orden: coincidencia exacta de lote → prefijo de lote → resto.
     */
    @Query(value = """
            SELECT * FROM qr_labels q
            WHERE UPPER(COALESCE(q.admin_status, 'ACTIVE')) = 'ACTIVE'
              AND (
                   q.lote ILIKE CONCAT('%', :q, '%')
                OR q.codigo ILIKE CONCAT('%', :q, '%')
                OR q.nombre ILIKE CONCAT('%', :q, '%')
              )
            ORDER BY
              CASE
                WHEN LOWER(q.lote) = LOWER(:q) THEN 0
                WHEN LOWER(q.lote) LIKE LOWER(CONCAT(:q, '%')) THEN 1
                WHEN LOWER(q.codigo) = LOWER(:q) THEN 2
                ELSE 3
              END,
              q.lote ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<QrLabel> searchSuggest(@Param("q") String q, @Param("limit") int limit);

    @Query(value = """
            SELECT CAST((created_at AT TIME ZONE 'America/Mexico_City') AS date) AS day,
                   COUNT(*) AS total
            FROM qr_labels
            WHERE created_at >= :from AND created_at < :to
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countLabelsGroupedByDay(@Param("from") Instant from, @Param("to") Instant to);
}
