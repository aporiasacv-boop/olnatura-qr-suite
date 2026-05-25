package com.company.olnaturaqr;

import com.company.olnaturaqr.core.integrity.SignatureDigest;
import com.company.olnaturaqr.core.integrity.StartupIntegrityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity
@Import(StartupIntegrityConfiguration.class)
public class OlnaturaQrApplication {
    private static final String INTEGRITY_ANCHOR = SignatureDigest.expectedDigest();

    public static void main(String[] args) {
        if (INTEGRITY_ANCHOR.isBlank()) {
            throw new IllegalStateException("Invalid internal integrity signature");
        }
        SpringApplication.run(OlnaturaQrApplication.class, args);
    }
}