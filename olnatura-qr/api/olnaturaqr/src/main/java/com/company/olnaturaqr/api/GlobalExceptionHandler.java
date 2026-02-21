package com.company.olnaturaqr.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

/**
 * Normaliza errores en JSON { "message": "...", "code": "..." }
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(ResponseStatusException.class)
    public void handleResponseStatus(ResponseStatusException ex, HttpServletResponse response) throws IOException {
        int status = ex.getStatusCode().value();

        HttpStatus httpStatus = HttpStatus.resolve(status);
        String reasonPhrase = (httpStatus != null) ? httpStatus.getReasonPhrase() : "Error";

        String message = (ex.getReason() != null && !ex.getReason().isBlank())
                ? ex.getReason()
                : reasonPhrase;

        String code = statusToCode(status);
        writeError(response, status, message, code);
    }

    private String statusToCode(int status) {
        if (status == 400) return "BAD_REQUEST";
        if (status == 401) return "UNAUTHORIZED";
        if (status == 403) return "FORBIDDEN";
        if (status == 404) return "NOT_FOUND";
        if (status == 409) return "CONFLICT";
        if (status >= 500) return "SERVER_ERROR";
        return "ERROR";
    }

    private void writeError(HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "message", message,
                "code", code
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}