package com.company.olnaturaqr.infra.dynamics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import org.springframework.web.client.RestClientException;

/**
 * Encapsula el flujo OAuth2 client_credentials de Azure AD (endpoint v1 con {@code resource})
 * usado contra Dynamics 365 F&amp;O. No almacena ni reutiliza tokens: cada llamada
 * {@link #requestAccessToken()} solicita uno nuevo y lo devuelve solo como valor local.
 */
@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class DynamicsOAuthTokenClient {

    private static final Logger log = LoggerFactory.getLogger(DynamicsOAuthTokenClient.class);

    private final DynamicsProperties properties;
    private final RestClient tokenRestClient;

    public DynamicsOAuthTokenClient(DynamicsProperties properties) {
        this.properties = properties;
        this.tokenRestClient = buildTokenClient(properties);
    }

    /**
     * Solicita un access_token nuevo. El valor retornado debe usarse solo para la búsqueda
     * en curso y no debe guardarse en campos de instancia, caché ni estáticos.
     */
    public String requestAccessToken() {
        validateOAuthConfig();

        String tokenUrl = resolveTokenUrl();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId().trim());
        form.add("client_secret", properties.getClientSecret().trim());
        form.add("resource", resolveResource());

        log.debug("Dynamics OAuth: solicitando access_token (client_credentials)");

        try {
            TokenResponse body = tokenRestClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            if (body == null || body.accessToken == null || body.accessToken.isBlank()) {
                log.warn("Dynamics OAuth: respuesta sin access_token");
                throw new DynamicsAuthException(
                        "Dynamics OAuth no devolvió access_token. Revisa la app registration en Azure AD.");
            }

            log.debug("Dynamics OAuth: token OK (expires_in={})", body.expiresIn);
            return body.accessToken.trim();
        } catch (DynamicsException ex) {
            throw ex;
        } catch (RestClientException ex) {
            // No registrar body ni headers (pueden incluir secretos / tokens).
            log.warn("Dynamics OAuth falló: tipo={}", ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.fromOAuth(ex);
        } catch (Exception ex) {
            log.warn("Dynamics OAuth error interno: tipo={}", ex.getClass().getSimpleName());
            throw DynamicsExceptionClassifier.unexpected("oauth", ex);
        }
    }

    private void validateOAuthConfig() {
        if (isBlank(properties.getTenantId())) {
            throw new DynamicsAuthException(
                    "Configuración Dynamics incompleta: falta APP_DYNAMICS_TENANT_ID");
        }
        if (isBlank(properties.getClientId())) {
            throw new DynamicsAuthException(
                    "Configuración Dynamics incompleta: falta APP_DYNAMICS_CLIENT_ID");
        }
        if (isBlank(properties.getClientSecret())) {
            throw new DynamicsAuthException(
                    "Configuración Dynamics incompleta: falta APP_DYNAMICS_CLIENT_SECRET");
        }
        if (isBlank(properties.getBaseUrl()) && isBlank(properties.getResource())) {
            throw new DynamicsAuthException(
                    "Configuración Dynamics incompleta: falta APP_DYNAMICS_BASE_URL o APP_DYNAMICS_RESOURCE");
        }
    }

    private String resolveTokenUrl() {
        if (!isBlank(properties.getTokenUrl())) {
            return properties.getTokenUrl().trim();
        }
        return "https://login.microsoftonline.com/"
                + properties.getTenantId().trim()
                + "/oauth2/token";
    }

    private String resolveResource() {
        if (!isBlank(properties.getResource())) {
            return properties.getResource().trim().replaceAll("/$", "");
        }
        return properties.getBaseUrl().trim().replaceAll("/$", "");
    }

    private static RestClient buildTokenClient(DynamicsProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) props.getReadTimeout().toMillis());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        public String accessToken;

        @JsonProperty("expires_in")
        public String expiresIn;

        @JsonProperty("token_type")
        public String tokenType;
    }
}
