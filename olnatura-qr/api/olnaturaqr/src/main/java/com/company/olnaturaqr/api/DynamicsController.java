package com.company.olnaturaqr.api;

import com.company.olnaturaqr.infra.dynamics.DynamicsOAuthTokenProvider;
import com.company.olnaturaqr.infra.dynamics.DynamicsPreviewService;
import com.company.olnaturaqr.infra.dynamics.DynamicsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/dynamics")
public class DynamicsController {

    private final DynamicsPreviewService previewService;
    private final DynamicsProperties properties;
    private final DynamicsOAuthTokenProvider tokenProvider;

    public DynamicsController(
            DynamicsPreviewService previewService,
            DynamicsProperties properties,
            @Autowired(required = false) DynamicsOAuthTokenProvider tokenProvider
    ) {
        this.previewService = previewService;
        this.properties = properties;
        this.tokenProvider = tokenProvider;
    }

    @GetMapping("/preview")
    public DynamicsPreviewResponse preview(
            @RequestParam(required = false) String itemNumber,
            @RequestParam(required = false) String lote
    ) {
        return previewService.fetchPreview(itemNumber, lote);
    }

    @GetMapping("/status")
    public DynamicsConnectionStatusResponse status() {
        boolean oauthConfigured = properties.isOAuthConfigured();
        boolean usesStatic = properties.getBearerToken() != null && !properties.getBearerToken().isBlank();
        Instant expiresAt = tokenProvider != null ? tokenProvider.getTokenExpiresAtUtc() : null;
        Instant lastRefresh = tokenProvider != null ? tokenProvider.getLastTokenRefreshAtUtc() : null;
        long refreshMs = tokenProvider != null ? tokenProvider.getLastTokenRefreshMs() : 0;
        String lastError = tokenProvider != null ? tokenProvider.getLastError() : null;
        boolean tokenValid = tokenProvider != null && tokenProvider.isTokenValid();
        boolean tokenExpired = tokenProvider != null && tokenProvider.isTokenExpired();
        long secondsLeft = tokenProvider != null ? tokenProvider.getSecondsUntilTokenExpiry() : 0;

        return new DynamicsConnectionStatusResponse(
                properties.getMode(),
                oauthConfigured,
                usesStatic,
                properties.isTokenRefreshScheduled(),
                properties.getTokenRefreshInterval().toString(),
                expiresAt != null ? expiresAt.toString() : null,
                lastRefresh != null ? lastRefresh.toString() : null,
                refreshMs,
                tokenValid,
                tokenExpired,
                lastError,
                secondsLeft
        );
    }
}
