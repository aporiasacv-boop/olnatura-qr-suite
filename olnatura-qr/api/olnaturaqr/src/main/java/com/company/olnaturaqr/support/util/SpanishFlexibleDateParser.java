package com.company.olnaturaqr.support.util;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Accepts ISO yyyy-MM-dd and slash forms d/M/yy, dd/MM/yy, d/MM/yyyy, dd/M/yyyy, dd/MM/yyyy (yy → 20yy).
 */
public final class SpanishFlexibleDateParser {

    private static final Pattern SLASH = Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})$");

    private SpanishFlexibleDateParser() {}

    public static LocalDate parseRequired(String raw, String fieldName) {
        LocalDate d = parseOptional(raw);
        if (d == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Fecha inválida: " + fieldName);
        }
        return d;
    }

    public static LocalDate parseOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        Matcher m = SLASH.matcher(s);
        if (!m.matches()) {
            return null;
        }
        int day = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        String yPart = m.group(3);
        int year = yPart.length() == 2 ? 2000 + Integer.parseInt(yPart) : Integer.parseInt(yPart);
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }
}
