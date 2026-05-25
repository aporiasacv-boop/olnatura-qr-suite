package com.company.olnaturaqr.infra.dynamics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.dynamics")
public class DynamicsProperties {

    private String mode = "mock";
    private String baseUrl = "https://olnatura-produccion.operations.dynamics.com";
    private String bearerToken;
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration tokenRefreshInterval = Duration.ofMinutes(65);
    private boolean tokenRefreshScheduled = true;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return normalizeResourceUrl(baseUrl);
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getTokenRefreshInterval() {
        return tokenRefreshInterval;
    }

    public void setTokenRefreshInterval(Duration tokenRefreshInterval) {
        this.tokenRefreshInterval = tokenRefreshInterval;
    }

    public boolean isTokenRefreshScheduled() {
        return tokenRefreshScheduled;
    }

    public void setTokenRefreshScheduled(boolean tokenRefreshScheduled) {
        this.tokenRefreshScheduled = tokenRefreshScheduled;
    }

    public String getTokenEndpoint() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("app.dynamics.tenant-id is required for OAuth");
        }
        return "https://login.microsoftonline.com/" + tenantId.trim() + "/oauth2/v2.0/token";
    }

    public String getOAuthScope() {
        return normalizeResourceUrl(baseUrl) + "/.default";
    }

    public boolean isOAuthConfigured() {
        return tenantId != null && !tenantId.isBlank()
                && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    private static String normalizeResourceUrl(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/data")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/data".length());
        }
        return trimmed;
    }
}
