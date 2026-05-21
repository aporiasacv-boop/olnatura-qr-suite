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

    public String getAccessToken() {
        String staticToken = properties.getBearerToken();
        if (staticToken != null && !staticToken.isBlank()) {
            return staticToken.trim();
        }
        if (!properties.isOAuthConfigured()) {
            throw new IllegalStateException(
                    "Dynamics real mode requires OAuth (tenant/client/secret) or APP_DYNAMICS_BEARER_TOKEN");
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

    private String refreshToken() {
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
                throw new IllegalStateException("Dynamics OAuth token response empty");
            }

            long expiresIn = parseExpiresIn(response.expiresIn);
            Instant validUntil = Instant.now().plusSeconds(Math.max(60, expiresIn - EXPIRY_BUFFER_SECONDS));
            cached = new CachedToken(response.accessToken.trim(), validUntil);
            log.info("Dynamics OAuth token renewed, expiresInSec={} cachedUntil={}", expiresIn, validUntil);
            return cached.accessToken();
        } catch (RestClientResponseException ex) {
            log.error("Dynamics OAuth token failed status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Dynamics OAuth token request failed: " + ex.getStatusCode().value(), ex);
        }
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

    private record CachedToken(String accessToken, Instant validUntil) {
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
