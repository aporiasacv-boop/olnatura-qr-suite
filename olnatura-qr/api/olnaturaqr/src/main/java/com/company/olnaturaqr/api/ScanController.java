package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.domain.scan.ScanEvent;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.presentation.RoleDisplayTranslator;
import com.company.olnaturaqr.support.presentation.UserDisplayHelper;
import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/scan")
public class ScanController {

    private final ScanEventRepository scanEventRepository;
    private final QrLabelRepository qrLabelRepository;
    private final UserRepository userRepository;

    public ScanController(
            ScanEventRepository scanEventRepository,
            QrLabelRepository qrLabelRepository,
            UserRepository userRepository
    ) {
        this.scanEventRepository = scanEventRepository;
        this.qrLabelRepository = qrLabelRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{lote}")
    public ResponseEntity<ScanDto.Response> create(
            @PathVariable String lote,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        String actualLote = resolveToLote(lote);

        ScanEvent ev = new ScanEvent();
        ev.setLote(actualLote);
        ev.setDeviceId(deviceId);

        if (principal != null && principal.id() != null) {
            ev.setScannedBy(principal.id());
        }

        ScanEvent saved = scanEventRepository.save(ev);
        User scanner = saved.getScannedBy() != null
                ? userRepository.findById(saved.getScannedBy()).orElse(null)
                : null;

        return ResponseEntity.ok(toResponse(saved, scanner));
    }

    @GetMapping("/{lote}")
    public ResponseEntity<?> list(@PathVariable String lote) {
        String actualLote = resolveToLote(lote);
        List<ScanEvent> events = scanEventRepository.findTop50ByLoteOrderByCreatedAtDesc(actualLote);
        Map<UUID, User> usersById = loadUsers(events);
        return ResponseEntity.ok(
                events.stream()
                        .map(ev -> toResponse(ev, usersById.get(ev.getScannedBy())))
                        .toList());
    }

    private Map<UUID, User> loadUsers(List<ScanEvent> events) {
        Set<UUID> ids = events.stream()
                .map(ScanEvent::getScannedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private ScanDto.Response toResponse(ScanEvent ev, User scanner) {
        String roleDisplay = scanner != null && scanner.getRole() != null
                ? RoleDisplayTranslator.translate(scanner.getRole().getName())
                : "—";
        return new ScanDto.Response(
                ev.getId(),
                ev.getLote(),
                ev.getScannedBy(),
                UserDisplayHelper.displayFromUser(scanner),
                roleDisplay,
                ev.getDeviceId(),
                ev.getCreatedAt());
    }

    private String resolveToLote(String raw) {
        String identifier = LoteExtractor.extract(raw).orElse(raw != null ? raw.trim() : "");
        if (identifier.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Identificador vacío");
        }
        QrLabel label = qrLabelRepository.findByPublicToken(identifier)
                .or(() -> qrLabelRepository.findByLote(identifier))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + identifier));
        LotOperationalGate.requireActive(label);
        return label.getLote();
    }
}
