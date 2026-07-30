package com.company.olnaturaqr.infra.dynamics;


public class DynamicsODataException extends DynamicsException {

    public static final String CODE = "DYNAMICS_ODATA_ERROR";

    public DynamicsODataException(String message) {
        super(message, CODE, 502);
    }

    public DynamicsODataException(String message, Throwable cause) {
        super(message, CODE, 502, cause);
    }
}
