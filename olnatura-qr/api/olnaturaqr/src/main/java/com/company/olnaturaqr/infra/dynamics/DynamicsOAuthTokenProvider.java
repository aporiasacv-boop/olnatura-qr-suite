package com.company.olnaturaqr.infra.dynamics;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class DynamicsOAuthTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(DynamicsOAuthTokenProvider.class);
    private static final long EXPIRY_BUFFER_SECONDS = 120;

    private final DynamicsProperties properties;
    private final RestClient tokenClient;
    private final Object refreshLock = new Object();
    private volatile CachedToken cached;
    private volatile Instant lastRefreshAt;
    private volatile long lastRefreshDurationMs;
    private volatile String lastError;

    public DynamicsOAuthTokenProvider(DynamicsProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        this.tokenClient = RestClient.builder()
                .baseUrl(properties.getTokenEndpoint())
                .requestFactory(requestFactory)
                .build();
    }

    public boolean usesStaticBearerToken() {
        String staticToken = properties.getBearerToken();
        return staticToken != null && !staticToken.isBlank();
    }

    public void refreshScheduled() {
        if (usesStaticBearerToken()) {
            return;
        }
        if (!properties.isOAuthConfigured()) {
            log.debug("Dynamics OAuth scheduled refresh skipped: OAuth not configured");
            return;
        }
        synchronized (refreshLock) {
            try {
                refreshToken();
            } catch (RuntimeException ex) {
                lastError = ex.getMessage();
                log.error("Dynamics OAuth scheduled refresh failed: {}", ex.getMessage());
            }
        }
    }

    public String getAccessToken() {
        if (usesStaticBearerToken()) {
            return properties.getBearerToken().trim();
        }
        if (!properties.isOAuthConfigured()) {
            throw new DynamicsException(
                    DynamicsErrorCode.DYNAMICS_NOT_CONFIGURED,
                    DynamicsErrorCode.DYNAMICS_NOT_CONFIGURED.getDefaultMessage(),
                    0
            );
        }
        CachedToken current = cached;
        if (current != null && current.isValid()) {
            return current.accessToken();
        }
        synchronized (refreshLock) {
            current = cached;
            if (current != null && current.isValid()) {
                return current.accessToken();
            }
            return refreshToken();
        }
    }

    public Instant getTokenExpiresAtUtc() {
        CachedToken current = cached;
        return current != null ? current.azureExpiresAt() : null;
    }

    public Instant getLastTokenRefreshAtUtc() {
        return lastRefreshAt;
    }

    public long getLastTokenRefreshMs() {
        return lastRefreshDurationMs;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isTokenValid() {
        if (usesStaticBearerToken()) {
            return true;
        }
        CachedToken current = cached;
        return current != null && current.isValid();
    }

    public boolean isTokenExpired() {
        if (usesStaticBearerToken()) {
            return false;
        }
        CachedToken current = cached;
        if (current == null) {
            return true;
        }
        return !Instant.now().isBefore(current.azureExpiresAt());
    }

    public long getSecondsUntilTokenExpiry() {
        CachedToken current = cached;
        if (current == null) {
            return 0;
        }
        long sec = current.azureExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, sec);
    }

    private String refreshToken() {
        long started = System.nanoTime();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId().trim());
        form.add("client_secret", properties.getClientSecret().trim());
        form.add("scope", properties.getOAuthScope());

        try {
            TokenResponse response = tokenClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken == null || response.accessToken.isBlank()) {
                throw new DynamicsException(
                        DynamicsErrorCode.OAUTH_FAILED,
                        "Dynamics OAuth token response empty",
                        elapsedMs(started)
                );
            }

            long expiresIn = parseExpiresIn(response.expiresIn);
            Instant azureExpiry = Instant.now().plusSeconds(expiresIn);
            Instant validUntil = Instant.now().plusSeconds(Math.max(60, expiresIn - EXPIRY_BUFFER_SECONDS));
            cached = new CachedToken(response.accessToken.trim(), validUntil, azureExpiry);
            lastRefreshAt = Instant.now();
            lastRefreshDurationMs = elapsedMs(started);
            lastError = null;
            log.info("Dynamics OAuth token renewed, expiresInSec={} azureExpiresAt={} cachedUntil={}",
                    expiresIn, azureExpiry, validUntil);
            return cached.accessToken();
        } catch (DynamicsException ex) {
            lastError = ex.getMessage();
            throw ex;
        } catch (RestClientResponseException ex) {
            lastError = "OAuth HTTP " + ex.getStatusCode().value();
            log.error("Dynamics OAuth token failed status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new DynamicsException(
                    DynamicsErrorCode.OAUTH_FAILED,
                    "Dynamics OAuth token request failed: " + ex.getStatusCode().value(),
                    elapsedMs(started),
                    ex
            );
        } catch (RuntimeException ex) {
            lastError = ex.getMessage();
            throw new DynamicsException(
                    DynamicsErrorCode.OAUTH_FAILED,
                    ex.getMessage(),
                    elapsedMs(started),
                    ex
            );
        }
    }

    private static long elapsedMs(long startedNano) {
        return (System.nanoTime() - startedNano) / 1_000_000;
    }

    private static long parseExpiresIn(String raw) {
        if (raw == null || raw.isBlank()) {
            return 3600;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 3600;
        }
    }

    private record CachedToken(String accessToken, Instant validUntil, Instant azureExpiresAt) {
        boolean isValid() {
            return accessToken != null && !accessToken.isBlank() && Instant.now().isBefore(validUntil);
        }
    }

    private static class TokenResponse {
        @JsonProperty("access_token")
        public String accessToken;

        @JsonProperty("expires_in")
        public String expiresIn;

        @JsonProperty("token_type")
        public String tokenType;
    }
}
