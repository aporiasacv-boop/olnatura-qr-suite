package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupDto;
import com.company.olnaturaqr.infra.dynamics.DynamicsLookupService;
import com.company.olnaturaqr.repository.QrLabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Al arrancar, detecta y corrige lotes cuyo {@code qr_labels.status} difiere del Estado Operativo
 * calculado actualmente desde Dynamics.
 */
@Component
@Order(100)
public class OperationalStatusBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperationalStatusBackfillRunner.class);

    private final boolean enabled;
    private final QrLabelRepository qrLabelRepository;
    private final DynamicsLookupService dynamicsLookupService;
    private final OperationalStatusSyncService operationalStatusSyncService;

    public OperationalStatusBackfillRunner(
            @Value("${app.dynamics.sync-operational-status-on-startup:true}") boolean enabled,
            QrLabelRepository qrLabelRepository,
            DynamicsLookupService dynamicsLookupService,
            OperationalStatusSyncService operationalStatusSyncService
    ) {
        this.enabled = enabled;
        this.qrLabelRepository = qrLabelRepository;
        this.dynamicsLookupService = dynamicsLookupService;
        this.operationalStatusSyncService = operationalStatusSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[EstadoOperativoSync] Backfill de arranque desactivado");
            return;
        }

        List<QrLabel> labels = qrLabelRepository.findAllByOrderByCreatedAtDesc();
        int checked = 0;
        int updated = 0;
        int unchanged = 0;
        int notFound = 0;
        int failed = 0;

        log.info("[EstadoOperativoSync] Backfill inicio total={}", labels.size());

        for (QrLabel snapshot : labels) {
            checked++;
            String lote = snapshot.getLote();
            try {
                Optional<DynamicsLookupDto> dyn = dynamicsLookupService.lookupByBatchNumber(lote);
                if (dyn.isEmpty()) {
                    notFound++;
                    continue;
                }
                var result = operationalStatusSyncService.applyDynamicsStatusById(
                        snapshot.getId(),
                        dyn.get().operationalStatus(),
                        OperationalStatusSyncService.REASON_BACKFILL,
                        null
                );
                if (result.updated()) {
                    updated++;
                } else {
                    unchanged++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("[EstadoOperativoSync] Backfill falló lote={} err={}", lote, e.toString());
            }
        }

        log.info(
                "[EstadoOperativoSync] Backfill fin checked={} updated={} unchanged={} notFoundInDynamics={} failed={}",
                checked,
                updated,
                unchanged,
                notFound,
                failed
        );
    }
}
