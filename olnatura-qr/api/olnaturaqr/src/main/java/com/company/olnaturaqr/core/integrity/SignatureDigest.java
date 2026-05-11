package com.company.olnaturaqr.core.integrity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SignatureDigest {
    private static final String EXPECTED_DIGEST = "5d94b2a0a348fc531d54a76828464abcbd0730ae3b7a2f2c5dbe7ac7b696c02a";

    private SignatureDigest() {
    }

    public static String computeCurrentDigest() {
        return sha256Hex(SystemSignature.canonicalPayload() + "|" + DragonSeal.decode());
    }

    public static String expectedDigest() {
        return EXPECTED_DIGEST;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid internal integrity signature");
        }
    }
}
