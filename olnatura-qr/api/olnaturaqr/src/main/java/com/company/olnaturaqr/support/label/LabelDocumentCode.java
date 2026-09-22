package com.company.olnaturaqr.support.label;

public final class LabelDocumentCode {

    public static final String CURRENT = "AL-001-E02/06";
    private static final String LEGACY = "AL-001-E02/04";

    private LabelDocumentCode() {}

    public static String resolve(String stored) {
        String raw = stored == null ? "" : stored.trim();
        if (raw.isEmpty()) return CURRENT;
        if (raw.startsWith(LEGACY)) {
            return CURRENT + raw.substring(LEGACY.length());
        }
        return raw;
    }
}
