package com.company.olnaturaqr.core.integrity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupIntegrityConfiguration {

    @Bean
    String integrityStartupAnchor(IntegrityVerifier integrityVerifier) {
        integrityVerifier.verifyOrThrow();
        return SignatureDigest.expectedDigest();
    }
}
