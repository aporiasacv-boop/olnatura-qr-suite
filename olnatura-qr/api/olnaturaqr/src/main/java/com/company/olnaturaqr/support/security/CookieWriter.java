package com.company.olnaturaqr.support.security;

import jakarta.servlet.http.HttpServletResponse;

public final class CookieWriter {

    private CookieWriter() {}

    public static void setJwtCookie(
            HttpServletResponse response,
            String cookieName,
            String jwt,
            boolean secure,
            String sameSite,
            long maxAgeSeconds
    ) {
        // HttpOnly + SameSite mitigar ataques XSS y CSRF
        String header = cookieName + "=" + jwt
                + "; Path=/"
                + "; Max-Age=" + maxAgeSeconds
                + "; HttpOnly"
                + "; SameSite=" + safeSameSite(sameSite);

        if (secure) header += "; Secure";

        response.addHeader("Set-Cookie", header);
    }

    public static void clearCookie(
            HttpServletResponse response,
            String cookieName,
            boolean secure,
            String sameSite
    ) {
        // valor vacío + Max-Age=0 para borrar correctamente en navegadores/proxies
        String header = cookieName + "="
                + "; Path=/"
                + "; Max-Age=0"
                + "; HttpOnly"
                + "; SameSite=" + safeSameSite(sameSite);

        if (secure) header += "; Secure";

        response.addHeader("Set-Cookie", header);
    }

    private static String safeSameSite(String sameSite) {
        if (sameSite == null) return "Lax";
        String v = sameSite.trim();
        return switch (v) {
            case "Lax", "Strict", "None" -> v;
            default -> "Lax";
        };
    }
}