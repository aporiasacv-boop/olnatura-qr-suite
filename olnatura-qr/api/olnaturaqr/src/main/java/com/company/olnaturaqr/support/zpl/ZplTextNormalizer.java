package com.company.olnaturaqr.support.zpl;

import java.text.Normalizer;


public final class ZplTextNormalizer {

    private ZplTextNormalizer() {}

    
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFKD);
        StringBuilder out = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); ) {
            int cp = decomposed.codePointAt(i);
            i += Character.charCount(cp);

            
            if (Character.getType(cp) == Character.NON_SPACING_MARK
                    || Character.getType(cp) == Character.COMBINING_SPACING_MARK
                    || Character.getType(cp) == Character.ENCLOSING_MARK) {
                continue;
            }

            
            if (cp == '^' || cp == '\\') {
                out.append(' ');
                continue;
            }

            
            if (cp >= 0x20 && cp <= 0x7E) {
                out.append((char) cp);
                continue;
            }

            
            if (Character.isWhitespace(cp)) {
                out.append(' ');
                continue;
            }

            
        }

        
        String collapsed = out.toString().replaceAll(" +", " ").trim();
        return collapsed;
    }
}
