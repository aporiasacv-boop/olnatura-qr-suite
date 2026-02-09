package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.scan.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScanEventRepository extends JpaRepository<ScanEvent, UUID> {
    List<ScanEvent> findTop50ByLoteOrderByCreatedAtDesc(String lote);
}