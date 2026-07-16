package com.company.olnaturaqr.api;

import com.company.olnaturaqr.support.metrics.MetricsService;
import com.company.olnaturaqr.support.metrics.MetricsService.OperationalMetrics;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public OperationalMetrics get(@RequestParam(defaultValue = "7") int days) {
        return metricsService.snapshot(days);
    }
}
