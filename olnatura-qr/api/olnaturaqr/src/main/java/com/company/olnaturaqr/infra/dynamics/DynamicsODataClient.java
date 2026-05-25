package com.company.olnaturaqr.infra.dynamics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class DynamicsODataClient {

    private static final Logger log = LoggerFactory.getLogger(DynamicsODataClient.class);

    private final RestClient restClient;
    private final DynamicsOAuthTokenProvider tokenProvider;
    private final DynamicsProperties properties;

    public DynamicsODataClient(DynamicsProperties properties, DynamicsOAuthTokenProvider tokenProvider) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public <T> List<T> query(
            String entityPath,
            String filter,
            int top,
            ParameterizedTypeReference<ODataListResponse<T>> typeRef
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/data/" + entityPath)
                .queryParam("cross-company", "true")
                .queryParam("$top", top);
        if (filter != null && !filter.isBlank()) {
            builder.queryParam("$filter", filter);
        }
        URI uri = builder.build().encode().toUri();

        long started = System.nanoTime();
        log.info("Dynamics OData GET {}", uri);

        try {
            String token = tokenProvider.getAccessToken();
            ODataListResponse<T> body = restClient.get()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .body(typeRef);

            long ms = elapsedMs(started);
            int count = body == null || body.value == null ? 0 : body.value.size();
            log.info("Dynamics OData OK entity={} rows={} elapsedMs={}", entityPath, count, ms);

            if (body == null || body.value == null) {
                return Collections.emptyList();
            }
            return body.value;
        } catch (DynamicsException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            long ms = elapsedMs(started);
            log.warn("Dynamics OData ERROR entity={} status={} elapsedMs={} body={}",
                    entityPath, ex.getStatusCode().value(), ms, truncate(ex.getResponseBodyAsString(), 500));
            throw mapHttpError(ex, ms);
        } catch (ResourceAccessException ex) {
            long ms = elapsedMs(started);
            log.warn("Dynamics OData ERROR entity={} elapsedMs={} message={}", entityPath, ms, ex.getMessage());
            throw mapNetworkError(ex, ms);
        } catch (RuntimeException ex) {
            long ms = elapsedMs(started);
            log.warn("Dynamics OData ERROR entity={} elapsedMs={} message={}", entityPath, ms, ex.getMessage());
            throw new DynamicsException(
                    DynamicsErrorCode.DYNAMICS_UNREACHABLE,
                    ex.getMessage(),
                    ms,
                    ex
            );
        }
    }

    private static DynamicsException mapHttpError(RestClientResponseException ex, long ms) {
        int status = ex.getStatusCode().value();
        if (status == 401 || status == 403) {
            if (tokenLikelyExpired(ex)) {
                return new DynamicsException(
                        DynamicsErrorCode.OAUTH_TOKEN_EXPIRED,
                        DynamicsErrorCode.OAUTH_TOKEN_EXPIRED.getDefaultMessage(),
                        ms,
                        ex
                );
            }
            return new DynamicsException(
                    DynamicsErrorCode.DYNAMICS_AUTH_REJECTED,
                    DynamicsErrorCode.DYNAMICS_AUTH_REJECTED.getDefaultMessage(),
                    ms,
                    ex
            );
        }
        if (status == 404) {
            return new DynamicsException(
                    DynamicsErrorCode.DYNAMICS_ENTITY_ERROR,
                    "Entidad o filtro OData no disponible en Dynamics",
                    ms,
                    ex
            );
        }
        if (status == 408 || status == 504) {
            return new DynamicsException(
                    DynamicsErrorCode.DYNAMICS_SLOW,
                    DynamicsErrorCode.DYNAMICS_SLOW.getDefaultMessage(),
                    ms,
                    ex
            );
        }
        return new DynamicsException(
                DynamicsErrorCode.DYNAMICS_ENTITY_ERROR,
                "Dynamics respondió con HTTP " + status,
                ms,
                ex
        );
    }

    private static boolean tokenLikelyExpired(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null) {
            return true;
        }
        String lower = body.toLowerCase();
        return lower.contains("expired") || lower.contains("lifetime") || lower.contains("invalid_token");
    }

    private static DynamicsException mapNetworkError(ResourceAccessException ex, long ms) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            return new DynamicsException(
                    DynamicsErrorCode.DYNAMICS_SLOW,
                    DynamicsErrorCode.DYNAMICS_SLOW.getDefaultMessage(),
                    ms,
                    ex
            );
        }
        return new DynamicsException(
                DynamicsErrorCode.DYNAMICS_UNREACHABLE,
                DynamicsErrorCode.DYNAMICS_UNREACHABLE.getDefaultMessage(),
                ms,
                ex
        );
    }

    static String escapeOdataLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private static long elapsedMs(long startedNano) {
        return (System.nanoTime() - startedNano) / 1_000_000;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public static class ODataListResponse<T> {
        public List<T> value;
    }
}
