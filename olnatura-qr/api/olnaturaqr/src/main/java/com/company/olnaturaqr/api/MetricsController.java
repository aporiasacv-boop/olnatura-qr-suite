package com.company.olnaturaqr.api;

import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.export.ExecutiveDashboardExportService;
import com.company.olnaturaqr.support.metrics.MetricsService;
import com.company.olnaturaqr.support.metrics.MetricsService.OperationalMetrics;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
public class MetricsController {

    private final MetricsService metricsService;
    private final ExecutiveDashboardExportService exportService;
    private final AuditService auditService;

    public MetricsController(
            MetricsService metricsService,
            ExecutiveDashboardExportService exportService,
            AuditService auditService
    ) {
        this.metricsService = metricsService;
        this.exportService = exportService;
        this.auditService = auditService;
    }

    @GetMapping
    public OperationalMetrics get(@RequestParam(defaultValue = "7") int days) {
        return metricsService.snapshot(days);
    }

    /**
     * Exportación tabular multi-hoja para Power BI (datos existentes en PostgreSQL).
     */
    @GetMapping(value = "/export/powerbi", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportPowerBi(@AuthenticationPrincipal AuthPrincipal principal) {
        try {
            var result = exportService.buildWorkbook();

            Map<String, Object> meta = new HashMap<>();
            meta.put("exportType", "EXECUTIVE_DASHBOARD_XLSX");
            meta.put("filename", ExecutiveDashboardExportService.FILENAME);
            meta.put("bytes", result.bytes().length);
            meta.put("labelsExported", result.labelsExported());
            meta.put("scansExported", result.scansExported());
            meta.put("auditsExported", result.auditsExported());
            meta.put("usersExported", result.usersExported());
            meta.put("requester", principal != null ? principal.username() : "anonymous");
            auditService.log(principal, "EXPORT_EXECUTIVE_DASHBOARD", null, meta, null);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + ExecutiveDashboardExportService.FILENAME + "\"")
                    .body(result.bytes());
        } catch (Exception ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "No se pudo generar el Excel", ex);
        }
    }
}
