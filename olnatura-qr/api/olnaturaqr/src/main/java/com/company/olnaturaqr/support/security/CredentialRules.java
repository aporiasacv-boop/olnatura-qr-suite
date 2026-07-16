package com.company.olnaturaqr.support.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de correo corporativo y contraseña para altas de usuario.
 * No aplica a OAuth/JWT/Dynamics.
 */
public final class CredentialRules {

    public static final String ALLOWED_EMAIL_DOMAIN = "@olnatura.com";

    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern BASIC_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private CredentialRules() {}

    public static boolean isAllowedEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String e = email.trim().toLowerCase(Locale.ROOT);
        if (!BASIC_EMAIL.matcher(e).matches()) return false;
        return e.endsWith(ALLOWED_EMAIL_DOMAIN);
    }

    public static String emailError(String email) {
        if (email == null || email.isBlank()) {
            return "El correo es obligatorio.";
        }
        String e = email.trim().toLowerCase(Locale.ROOT);
        if (!BASIC_EMAIL.matcher(e).matches()) {
            return "El correo no tiene un formato válido.";
        }
        if (!e.endsWith(ALLOWED_EMAIL_DOMAIN)) {
            return "Solo se permiten correos " + ALLOWED_EMAIL_DOMAIN + ".";
        }
        return null;
    }

    public static boolean isValidPassword(String password) {
        return passwordIssues(password).isEmpty();
    }

    public static List<String> passwordIssues(String password) {
        List<String> issues = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            issues.add("La contraseña es obligatoria.");
            return issues;
        }
        if (password.length() < 8) {
            issues.add("Mínimo 8 caracteres.");
        }
        if (!HAS_UPPER.matcher(password).find()) {
            issues.add("Al menos una letra mayúscula.");
        }
        if (!HAS_LOWER.matcher(password).find()) {
            issues.add("Al menos una letra minúscula.");
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            issues.add("Al menos un número.");
        }
        return issues;
    }

    public static String passwordError(String password) {
        List<String> issues = passwordIssues(password);
        if (issues.isEmpty()) return null;
        return String.join(" ", issues);
    }
}
