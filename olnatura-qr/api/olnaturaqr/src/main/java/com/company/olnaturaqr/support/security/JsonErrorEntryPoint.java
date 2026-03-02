package com.company.olnaturaqr.support.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Devuelve errores 401/403 en JSON: { "message": "...", "code": "..." }
 */
public class JsonErrorEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String UNAUTHORIZED_MESSAGE = "No autenticado. Inicia sesión.";
    private static final String FORBIDDEN_MESSAGE = "No tienes permiso para acceder a este recurso.";
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String FORBIDDEN_CODE = "FORBIDDEN";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        respond(request, response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_MESSAGE, UNAUTHORIZED_CODE);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        respond(request, response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_MESSAGE, FORBIDDEN_CODE);
    }

    private void respond(HttpServletRequest request, HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String path = (request != null && request.getRequestURI() != null) ? request.getRequestURI() : "";
        Map<String, Object> body = Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "path", path,
                "status", status,
                "error", code,
                "message", message
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
