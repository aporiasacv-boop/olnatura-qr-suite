package com.company.olnaturaqr.infra.dynamics;

/**
 * Timeout de conexión o lectura hacia Azure AD u OData Dynamics.
 */
public class DynamicsTimeoutException extends DynamicsException {

    public static final String CODE = "DYNAMICS_TIMEOUT";

    public DynamicsTimeoutException(String message) {
        super(message, CODE, 504);
    }

    public DynamicsTimeoutException(String message, Throwable cause) {
        super(message, CODE, 504, cause);
    }
}
