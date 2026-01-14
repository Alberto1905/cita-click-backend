package com.reservas.exception;

/**
 * Excepción personalizada para errores relacionados con notificaciones.
 */
public class NotificationException extends RuntimeException {

    private final String errorCode;
    private final String providerMessage;

    public NotificationException(String message) {
        super(message);
        this.errorCode = "NOTIFICATION_ERROR";
        this.providerMessage = null;
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NOTIFICATION_ERROR";
        this.providerMessage = null;
    }

    public NotificationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.providerMessage = null;
    }

    public NotificationException(String message, String errorCode, String providerMessage) {
        super(message);
        this.errorCode = errorCode;
        this.providerMessage = providerMessage;
    }

    public NotificationException(String message, String errorCode, Throwable cause) {
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
