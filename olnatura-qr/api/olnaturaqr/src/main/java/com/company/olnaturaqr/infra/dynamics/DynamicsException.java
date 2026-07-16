package com.company.olnaturaqr.infra.dynamics;

/**
 * Error de integración con Dynamics 365. Subtipos separan OAuth, OData, timeout e interno.
 * El campo {@code errorCode} se expone al frontend en el JSON de error (mismo contrato).
 */
public abstract class DynamicsException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    protected DynamicsException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected DynamicsException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
