package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.scan.ScanEvent;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/scan")
public class ScanController {

    private final ScanEventRepository scanEventRepository;
    private final QrLabelRepository qrLabelRepository;

    public ScanController(ScanEventRepository scanEventRepository, QrLabelRepository qrLabelRepository) {
        this.scanEventRepository = scanEventRepository;
        this.qrLabelRepository = qrLabelRepository;
    }

    @PostMapping("/{lote}")
    public ResponseEntity<ScanDto.Response> create(
            @PathVariable String lote,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        String normalized = (lote == null) ? "" : lote.trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + lote);
        }

        // valida que exista el lote
        qrLabelRepository.findByLote(normalized)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + normalized));

        ScanEvent ev = new ScanEvent();
        ev.setLote(normalized);
        ev.setDeviceId(deviceId);

        // cookie JWT
        if (principal != null && principal.id() != null) {
            ev.setScannedBy(principal.id());
        }

        ScanEvent saved = scanEventRepository.save(ev);

        return ResponseEntity.ok(new ScanDto.Response(
                saved.getId(),
                saved.getLote(),
                saved.getScannedBy(),
                saved.getDeviceId(),
                saved.getCreatedAt()
        ));
    }

    @GetMapping("/{lote}")
    public ResponseEntity<?> list(@PathVariable String lote) {
        String normalized = (lote == null) ? "" : lote.trim();
        return ResponseEntity.ok(
                scanEventRepository.findTop50ByLoteOrderByCreatedAtDesc(normalized)
                        .stream()
                        .map(ev -> new ScanDto.Response(
                                ev.getId(),
                                ev.getLote(),
                                ev.getScannedBy(),
                                ev.getDeviceId(),
                                ev.getCreatedAt()
                        ))
                        .toList()
        );
    }
}