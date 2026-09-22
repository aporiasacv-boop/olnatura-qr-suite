package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.report.ProblemReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, UUID> {
    List<ProblemReport> findTop100ByOrderByCreatedAtDesc();
    List<ProblemReport> findTop100ByStatusOrderByCreatedAtDesc(String status);

    void deleteByLote(String lote);
}
