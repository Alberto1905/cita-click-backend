package com.reservas.exception;

/**
 * Excepción personalizada para errores relacionados con billing/suscripciones.
 */
public class BillingException extends RuntimeException {

    private final String errorCode;

    public BillingException(String message) {
        super(message);
        this.errorCode = "BILLING_ERROR";
    }

    public BillingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BILLING_ERROR";
    }

    public BillingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BillingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
