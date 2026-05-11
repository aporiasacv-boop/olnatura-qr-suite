package com.company.olnaturaqr.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        String name,
        boolean secure,
        String sameSite,
        long maxAgeSeconds
) {}