package com.company.olnaturaqr.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProps(String allowedOrigins) {

    public List<String> allowedOriginsList() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of();
        }

        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(CorsProps::stripTrailingSlash)
                .toList();
    }

    private static String stripTrailingSlash(String s) {
        String v = s.trim();
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }
}