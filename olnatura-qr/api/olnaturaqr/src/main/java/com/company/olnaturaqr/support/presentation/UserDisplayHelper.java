package com.company.olnaturaqr.support.presentation;

import com.company.olnaturaqr.domain.user.User;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Resuelve identificadores de usuario a texto legible para presentación.
 */
public final class UserDisplayHelper {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private UserDisplayHelper() {
    }

    public static boolean looksLikeUuid(String value) {
        return value != null && UUID_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * Formato de presentación a partir del username (sin campo nombre completo en BD).
     * Usernames con punto (ej. Virginia.Amaro) se muestran como nombre legible (Virginia Amaro).
     */
    public static String formatUsernameForDisplay(String username) {
        if (username == null || username.isBlank()) {
            return "—";
        }
        String trimmed = username.trim();
        if (looksLikeUuid(trimmed)) {
            return "—";
        }
        if (trimmed.contains(".") && !trimmed.contains("@")) {
            String[] parts = trimmed.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part.isBlank()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(capitalizeWord(part));
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return trimmed;
    }

    public static String displayFromUser(User user) {
        if (user == null) {
            return "—";
        }
        return formatUsernameForDisplay(user.getUsername());
    }

    public static String displayFromUsernameOrEmail(String username, String email) {
        if (username != null && !username.isBlank() && !looksLikeUuid(username)) {
            return formatUsernameForDisplay(username);
        }
        if (email != null && !email.isBlank() && !looksLikeUuid(email)) {
            return email.trim();
        }
        return "—";
    }

    private static String capitalizeWord(String word) {
        if (word.isEmpty()) return word;
        if (word.length() == 1) {
            return word.toUpperCase(Locale.ROOT);
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
    }
}
