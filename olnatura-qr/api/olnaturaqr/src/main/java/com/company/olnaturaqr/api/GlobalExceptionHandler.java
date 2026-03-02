package com.company.olnaturaqr.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Normaliza errores en JSON: timestamp, path, status, error, message (en español).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<Integer, String> MENSAGES_ES = Map.of(
            400, "Solicitud inválida",
            401, "No autenticado",
            403, "Acceso denegado",
            404, "No encontrado",
            409, "Conflicto",
            500, "Error interno del servidor"
    );

    @ExceptionHandler(ResponseStatusException.class)
    public void handleResponseStatus(ResponseStatusException ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int status = ex.getStatusCode().value();
        String message = (ex.getReason() != null && !ex.getReason().isBlank())
                ? ex.getReason()
                : MENSAGES_ES.getOrDefault(status, "Error");
        String code = statusToCode(status);
        writeError(request, response, status, message, code);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + (e.getDefaultMessage() != null ? e.getDefaultMessage() : "inválido"))
                .collect(Collectors.joining("; "));
        if (message.isBlank()) message = "Datos de validación inválidos";
        writeError(request, response, 400, message, "BAD_REQUEST");
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

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "path", request != null ? request.getRequestURI() : "",
                "status", status,
                "error", code,
                "message", message
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}