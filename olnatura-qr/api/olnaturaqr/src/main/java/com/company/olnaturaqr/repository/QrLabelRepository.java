package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.qr.QrLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrLabelRepository extends JpaRepository<QrLabel, UUID> {
    Optional<QrLabel> findByLote(String lote);
    Optional<QrLabel> findByPublicToken(String publicToken);
    boolean existsByLote(String lote);
}