package com.company.olnaturaqr.support.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class ProdConfigGuard {

    private final Environment env;
    private final CorsProps corsProps;

    public ProdConfigGuard(Environment env, CorsProps corsProps) {
        this.env = env;
        this.corsProps = corsProps;
    }

    @PostConstruct
    void validate() {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            if (corsProps.allowedOriginsList() == null || corsProps.allowedOriginsList().isEmpty()) {
                throw new IllegalStateException("CORS origins must be set in PROD (app.cors.allowed-origins)");
            }
        }
    }
}