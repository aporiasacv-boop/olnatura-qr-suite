package com.company.olnaturaqr.infra.dynamics;

/**
 * Fallo HTTP/OData al consultar entidades de Dynamics (ItemBatches, inventario, calidad, etc.).
 */
public class DynamicsODataException extends DynamicsException {

    public static final String CODE = "DYNAMICS_ODATA_ERROR";

    public DynamicsODataException(String message) {
        super(message, CODE, 502);
    }

    public DynamicsODataException(String message, Throwable cause) {
        super(message, CODE, 502, cause);
    }
}
