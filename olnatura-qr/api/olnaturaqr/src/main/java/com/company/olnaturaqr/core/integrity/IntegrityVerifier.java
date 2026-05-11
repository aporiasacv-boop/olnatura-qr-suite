package com.company.olnaturaqr.core.integrity;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class IntegrityVerifier {
    @PostConstruct
    void verifyOnStartup() {
        verifyOrThrow();
    }

    public void verifyOrThrow() {
        String current = SignatureDigest.computeCurrentDigest();
        String expected = SignatureDigest.expectedDigest();
        if (!expected.equals(current)) {
            throw new IllegalStateException("Invalid internal integrity signature");
        }
    }
}
