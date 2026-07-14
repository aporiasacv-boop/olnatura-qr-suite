package com.company.olnaturaqr.infra.dynamics;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Clasifica fallos RestClient de Dynamics/Azure AD sin registrar secretos ni bodies.
 */
final class DynamicsExceptionClassifier {

    private DynamicsExceptionClassifier() {
    }

    static DynamicsException fromOAuth(RestClientException ex) {
        if (isTimeout(ex)) {
            return new DynamicsTimeoutException(
                    "Timeout al solicitar token de Dynamics (Azure AD). Intenta de nuevo.",
                    ex);
        }
        if (ex instanceof RestClientResponseException http) {
            return new DynamicsAuthException(
                    "No se pudo autenticar con Dynamics 365 (Azure AD HTTP "
                            + http.getStatusCode().value() + "). Verifica credenciales OAuth.",
                    ex);
        }
        return new DynamicsAuthException(
                "No se pudo autenticar con Dynamics 365 (conexión con Azure AD).",
                ex);
    }

    static DynamicsException fromOData(String entity, String lote, RestClientException ex) {
        String loteSafe = lote != null ? lote : "?";
        if (isTimeout(ex)) {
            return new DynamicsTimeoutException(
                    "Timeout al consultar Dynamics (" + entity + ") para el lote " + loteSafe + ".",
                    ex);
        }
        if (ex instanceof RestClientResponseException http) {
            return new DynamicsODataException(
                    "Error OData Dynamics en " + entity + " (HTTP "
                            + http.getStatusCode().value() + ") para el lote " + loteSafe + ".",
                    ex);
        }
        if (ex instanceof ResourceAccessException) {
            return new DynamicsODataException(
                    "No hay conexión con Dynamics al consultar " + entity + " (lote " + loteSafe + ").",
                    ex);
        }
        return new DynamicsODataException(
                "Error al consultar Dynamics (" + entity + ") para el lote " + loteSafe + ".",
                ex);
    }

    static DynamicsException unexpected(String context, Throwable ex) {
        return new DynamicsInternalException(
                "Error interno al integrar Dynamics" + (context != null && !context.isBlank() ? " (" + context + ")" : "") + ".",
                ex);
    }

    static boolean isTimeout(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof SocketTimeoutException || cur instanceof TimeoutException) {
                return true;
            }
            if (cur instanceof ResourceAccessException) {
                String msg = cur.getMessage();
                if (msg != null) {
                    String m = msg.toLowerCase();
                    if (m.contains("timed out") || m.contains("timeout")) {
                        return true;
                    }
                }
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("read timed out")
                        || m.contains("connect timed out")
                        || m.contains("connection timed out")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
