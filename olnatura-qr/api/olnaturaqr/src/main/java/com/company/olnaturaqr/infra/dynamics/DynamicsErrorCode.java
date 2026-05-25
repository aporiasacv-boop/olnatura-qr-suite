package com.company.olnaturaqr.infra.dynamics;

import org.springframework.http.HttpStatus;

public enum DynamicsErrorCode {

    OAUTH_CONNECTING(HttpStatus.SERVICE_UNAVAILABLE, "Estamos estableciendo la conexión con Dynamics"),
    OAUTH_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo obtener acceso a Dynamics"),
    OAUTH_TOKEN_EXPIRED(HttpStatus.SERVICE_UNAVAILABLE, "La sesión con Dynamics expiró y debe renovarse"),
    DYNAMICS_SLOW(HttpStatus.GATEWAY_TIMEOUT, "Dynamics está tardando más de lo esperado"),
    DYNAMICS_UNREACHABLE(HttpStatus.BAD_GATEWAY, "No hay conexión con el servidor de Dynamics"),
    DYNAMICS_AUTH_REJECTED(HttpStatus.BAD_GATEWAY, "Dynamics rechazó la autenticación"),
    DYNAMICS_ENTITY_ERROR(HttpStatus.BAD_GATEWAY, "La consulta a Dynamics no pudo completarse"),
    DYNAMICS_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "La integración con Dynamics no está configurada");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    DynamicsErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
