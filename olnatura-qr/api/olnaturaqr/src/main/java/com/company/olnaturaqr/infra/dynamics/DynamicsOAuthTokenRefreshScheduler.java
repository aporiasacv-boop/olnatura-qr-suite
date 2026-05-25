package com.company.olnaturaqr.infra.dynamics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "real")
public class DynamicsOAuthTokenRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(DynamicsOAuthTokenRefreshScheduler.class);

    private final DynamicsProperties properties;
    private final DynamicsOAuthTokenProvider tokenProvider;

    public DynamicsOAuthTokenRefreshScheduler(
            DynamicsProperties properties,
            DynamicsOAuthTokenProvider tokenProvider
    ) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStartup() {
        if (!properties.isTokenRefreshScheduled()) {
            return;
        }
        log.info("Dynamics OAuth warm-up on startup");
        tokenProvider.refreshScheduled();
    }

    @Scheduled(
            initialDelayString = "${app.dynamics.token-refresh-interval:65m}",
            fixedDelayString = "${app.dynamics.token-refresh-interval:65m}"
    )
    public void refreshTokenPeriodically() {
        if (!properties.isTokenRefreshScheduled()) {
            return;
        }
        log.info("Dynamics OAuth periodic refresh (interval={})", properties.getTokenRefreshInterval());
        tokenProvider.refreshScheduled();
    }
}
