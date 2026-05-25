package com.company.olnaturaqr.infra.dynamics;

public class DynamicsException extends RuntimeException {

    private final DynamicsErrorCode code;
    private final long elapsedMs;

    public DynamicsException(DynamicsErrorCode code, String message, long elapsedMs) {
        super(message != null && !message.isBlank() ? message : code.getDefaultMessage());
        this.code = code;
        this.elapsedMs = Math.max(0, elapsedMs);
    }

    public DynamicsException(DynamicsErrorCode code, String message, long elapsedMs, Throwable cause) {
        super(message != null && !message.isBlank() ? message : code.getDefaultMessage(), cause);
        this.code = code;
        this.elapsedMs = Math.max(0, elapsedMs);
    }

    public DynamicsErrorCode getCode() {
        return code;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
