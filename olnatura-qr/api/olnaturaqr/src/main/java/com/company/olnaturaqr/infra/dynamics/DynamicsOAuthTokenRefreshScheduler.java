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
@ConditionalOnProperty(prefix = "app.dynamics", name = "token-refresh-scheduled", havingValue = "true")
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
        if (!shouldRunRefresh()) {
            return;
        }
        log.info("Dynamics OAuth warm-up on startup");
        tokenProvider.refreshScheduled();
    }

    @Scheduled(
            initialDelayString = "${app.dynamics.token-refresh-interval:50m}",
            fixedDelayString = "${app.dynamics.token-refresh-interval:50m}"
    )
    public void refreshTokenPeriodically() {
        if (!shouldRunRefresh()) {
            return;
        }
        if (tokenProvider.isTokenValid()
                && tokenProvider.getSecondsUntilTokenExpiry() > properties.getTokenRefreshInterval().getSeconds()) {
            return;
        }
        log.info("Dynamics OAuth periodic refresh (interval={})", properties.getTokenRefreshInterval());
        tokenProvider.refreshScheduled();
    }

    private boolean shouldRunRefresh() {
        if (tokenProvider.usesStaticBearerToken()) {
            return false;
        }
        return properties.isOAuthConfigured();
    }
}
