package com.company.olnaturaqr.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Unauthenticated connectivity check for network diagnostics.
 * Use from another PC to verify backend is reachable (firewall/subnet testing).
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicConnectivityController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "olnaturaqr",
                "time", Instant.now().toString()
        ));
    }
}
