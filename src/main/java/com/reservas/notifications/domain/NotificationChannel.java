package com.reservas.notifications.domain;

/**
 * Enumeración de canales de notificación soportados.
 */
public enum NotificationChannel {
    /**
     * Correo electrónico
     */
    EMAIL,

    /**
     * SMS (mensaje de texto)
     */
    SMS,

    /**
     * WhatsApp
     */
    WHATSAPP,

    /**
     * Push notification (móvil/web)
     */
    PUSH,

    /**
     * Notificación in-app
     */
    IN_APP
}
