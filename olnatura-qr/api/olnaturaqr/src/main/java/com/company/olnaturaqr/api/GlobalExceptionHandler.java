package com.company.olnaturaqr.api;

import com.company.olnaturaqr.infra.dynamics.DynamicsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<Integer, String> MENSAGES_ES = Map.of(
            400, "Solicitud inválida",
            401, "No autenticado",
            403, "Acceso denegado",
            404, "No encontrado",
            409, "Conflicto",
            502, "Error de servicios externos",
            504, "Tiempo de espera agotado",
            500, "Error interno del servidor"
    );

    @ExceptionHandler(DynamicsException.class)
    public void handleDynamics(DynamicsException ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Error al comunicar con Dynamics 365";
        log.warn("Dynamics error code={} status={} path={} type={}",
                ex.getErrorCode(),
                ex.getHttpStatus(),
                request != null ? request.getRequestURI() : "",
                ex.getClass().getSimpleName());
        writeError(request, response, ex.getHttpStatus(), message, ex.getErrorCode());
    }

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
        if (status == 502) return "BAD_GATEWAY";
        if (status == 504) return "GATEWAY_TIMEOUT";
        if (status >= 500) return "SERVER_ERROR";
        return "ERROR";
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("path", request != null ? request.getRequestURI() : "");
        body.put("status", status);
        body.put("error", code);
        body.put("message", message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
