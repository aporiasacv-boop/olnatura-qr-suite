package com.company.olnaturaqr.infra.dynamics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
                .queryParam("$top", top);
        if (filter != null && !filter.isBlank()) {
            builder.queryParam("$filter", filter);
        }
        URI uri = builder.build().encode().toUri();

        long started = System.nanoTime();
        log.info("Dynamics OData GET {}", uri);

        try {
            ODataListResponse<T> body = restClient.get()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(tokenProvider.getAccessToken()))
                    .retrieve()
                    .body(typeRef);

            long ms = (System.nanoTime() - started) / 1_000_000;
            int count = body == null || body.value == null ? 0 : body.value.size();
            log.info("Dynamics OData OK entity={} rows={} elapsedMs={}", entityPath, count, ms);

            if (body == null || body.value == null) {
                return Collections.emptyList();
            }
            return body.value;
        } catch (RestClientResponseException ex) {
            long ms = (System.nanoTime() - started) / 1_000_000;
            log.warn("Dynamics OData ERROR entity={} status={} elapsedMs={} body={}",
                    entityPath, ex.getStatusCode().value(), ms, truncate(ex.getResponseBodyAsString(), 500));
            return Collections.emptyList();
        } catch (Exception ex) {
            long ms = (System.nanoTime() - started) / 1_000_000;
            log.warn("Dynamics OData ERROR entity={} elapsedMs={} message={}", entityPath, ms, ex.getMessage());
            return Collections.emptyList();
        }
    }

    static String escapeOdataLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public static class ODataListResponse<T> {
        public List<T> value;
    }
}
