package com.company.olnaturaqr.api;

public record DynamicsConnectionStatusResponse(
        String mode,
        boolean oauthConfigured,
        boolean usesStaticBearerToken,
        boolean scheduledRefreshEnabled,
        String tokenRefreshInterval,
        String tokenExpiresAtUtc,
        String lastTokenRefreshAtUtc,
        long lastTokenRefreshMs,
        boolean tokenValid,
        boolean tokenExpired,
        String lastError,
        long secondsUntilTokenExpiry
) {}
