package com.company.olnaturaqr.support.zpl;

import java.text.Normalizer;

/**
 * Normaliza textos para impresoras Zebra ZPL (ASCII seguro).
 * Una sola pasada: quita acentos y elimina caracteres que generan glifos raros (¾, □, ? mal mapeados, etc.).
 */
public final class ZplTextNormalizer {

    private ZplTextNormalizer() {}

    /**
     * Convierte a ASCII imprimible: Á→A, ñ→n, etc. Elimina el resto de no-ASCII
     * y caracteres de control. Reemplaza ^ y \ (reservados ZPL) por espacio.
     */
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // NFKD: descompone acentos (É → E + ́)
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFKD);
        StringBuilder out = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); ) {
            int cp = decomposed.codePointAt(i);
            i += Character.charCount(cp);

            // Marcas diacríticas (acentos)
            if (Character.getType(cp) == Character.NON_SPACING_MARK
                    || Character.getType(cp) == Character.COMBINING_SPACING_MARK
                    || Character.getType(cp) == Character.ENCLOSING_MARK) {
                continue;
            }

            // Ñ/ñ: NFKD con N + ~ ; al quitar diacríticos queda N/n — ya cubierto.
            // Casos especiales españoles ya resueltos por NFKD + strip marks.

            if (cp == '^' || cp == '\\') {
                out.append(' ');
                continue;
            }

            // ASCII imprimible (espacio..~)
            if (cp >= 0x20 && cp <= 0x7E) {
                out.append((char) cp);
                continue;
            }

            // Espacios no estándar → espacio
            if (Character.isWhitespace(cp)) {
                out.append(' ');
                continue;
            }

            // Cualquier otro (¾, □, emoji, tipografía, etc.) → omitir
        }

        // Colapsar espacios múltiples
        String collapsed = out.toString().replaceAll(" +", " ").trim();
        return collapsed;
    }
}
