package com.company.olnaturaqr.infra.dynamics;

/**
 * Fallo al autenticar con Azure AD / OAuth (client credentials) hacia Dynamics.
 */
public class DynamicsAuthException extends DynamicsException {

    public static final String CODE = "DYNAMICS_AUTH_ERROR";

    public DynamicsAuthException(String message) {
        super(message, CODE, 502);
    }

    public DynamicsAuthException(String message, Throwable cause) {
        super(message, CODE, 502, cause);
    }
}
