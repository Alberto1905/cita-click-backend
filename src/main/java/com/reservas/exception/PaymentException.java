package com.reservas.exception;

/**
 * Excepción personalizada para errores relacionados con pagos.
 *
 * Se lanza cuando:
 * - Falla la creación de un PaymentIntent
 * - Error al confirmar un pago
 * - Problema con una cuenta conectada
 * - Cualquier error relacionado con Stripe u otro proveedor de pagos
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String providerMessage;

    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
        this.providerMessage = null;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_ERROR";
        this.providerMessage = null;
    }

    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.providerMessage = null;
    }

    public PaymentException(String message, String errorCode, String providerMessage) {
        super(message);
        this.errorCode = errorCode;
        this.providerMessage = providerMessage;
    }

    public PaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.providerMessage = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getProviderMessage() {
        return providerMessage;
    }
}
