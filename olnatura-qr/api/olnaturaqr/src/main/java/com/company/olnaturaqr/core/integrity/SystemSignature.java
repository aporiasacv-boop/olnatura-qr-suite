package com.company.olnaturaqr.core.integrity;

import java.util.List;

public final class SystemSignature {
    public static final String AASC = "AASC";
    public static final String ID_161101 = "161101";
    public static final String ID_270625 = "270625";
    public static final String JUSC = "JUSC";
    public static final String SCF = "SCF";
    public static final String LOC = "LOC";
    public static final String HASU = "HASU";

    private static final List<String> TOKENS = List.of(
            AASC,
            ID_161101,
            ID_270625,
            JUSC,
            SCF,
            LOC,
            HASU
    );

    private SystemSignature() {
    }

    public static String canonicalPayload() {
        return String.join("|", TOKENS);
    }
}
