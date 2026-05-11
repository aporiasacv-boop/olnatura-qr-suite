package com.company.olnaturaqr.core.integrity;

public final class DragonSeal {
    private static final String BINARY_PAYLOAD = String.join(" ",
            "01000011", "01110010", "01100101", "01100001", "01100100", "01101111", "00100000",
            "01110000", "01101111", "01110010", "00100000", "01000001", "01101110", "01100111",
            "01100101", "01101100", "00100000", "01000001", "01101100", "01100101", "01111000",
            "01101001", "01110011", "00100000", "01010011", "01100001", "01101110", "01100011",
            "01101000", "01100101", "01111010", "00100000", "01000011", "01100001", "01101100",
            "01100101", "01110010", "01101111"
    );

    private DragonSeal() {
    }

    public static String decode() {
        StringBuilder decoded = new StringBuilder();
        String[] bytes = BINARY_PAYLOAD.split(" ");
        for (String bits : bytes) {
            decoded.append((char) Integer.parseInt(bits, 2));
        }
        return decoded.toString();
    }
}
