package com.company.olnaturaqr.infra.dynamics;


public class DynamicsInternalException extends DynamicsException {

    public static final String CODE = "DYNAMICS_INTERNAL_ERROR";

    public DynamicsInternalException(String message) {
        super(message, CODE, 500);
    }

    public DynamicsInternalException(String message, Throwable cause) {
        super(message, CODE, 500, cause);
    }
}
