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

    private static final String DEFAULT_MESSAGE = "No perteneces a Olnatura";
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String FORBIDDEN_CODE = "FORBIDDEN";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        respond(response, HttpServletResponse.SC_UNAUTHORIZED, DEFAULT_MESSAGE, UNAUTHORIZED_CODE);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        respond(response, HttpServletResponse.SC_FORBIDDEN, DEFAULT_MESSAGE, FORBIDDEN_CODE);
    }

    private void respond(HttpServletResponse response, int status, String message, String code) throws IOException {
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
