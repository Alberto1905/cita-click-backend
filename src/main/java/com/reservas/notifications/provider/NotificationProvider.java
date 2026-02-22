package com.reservas.notifications.provider;

import com.reservas.notifications.domain.Notification;
import com.reservas.notifications.domain.NotificationChannel;
import com.reservas.notifications.domain.NotificationResult;
import com.reservas.notifications.dto.SendNotificationRequest;

import java.util.List;

/**
 * Interfaz base para proveedores de notificaciones.
 *
 * Esta interfaz abstrae el envío de notificaciones sin importar el canal:
 * - Email (Resend, SES, etc.)
 * - SMS (Twilio, etc.)
 * - WhatsApp (Twilio, etc.)
 * - Push notifications
 *
 * Permite cambiar de proveedor fácilmente sin afectar la lógica de negocio.
 */
public interface NotificationProvider {

    /**
     * Envía una notificación.
     *
     * @param request Datos de la notificación (destinatario, contenido, canal)
     * @return NotificationResult con ID y estado del envío
     * @throws NotificationException si hay error en el envío
     */
    NotificationResult send(SendNotificationRequest request);

    /**
     * Envía notificaciones en lote (batch).
     *
     * Más eficiente cuando se necesita enviar la misma notificación
     * a múltiples destinatarios.
     *
     * @param requests Lista de notificaciones a enviar
     * @return Lista de resultados (mismo orden que las requests)
     * @throws NotificationException si hay error
     */
    List<NotificationResult> sendBatch(List<SendNotificationRequest> requests);

    /**
     * Verifica el estado de una notificación enviada.
     *
     * Útil para tracking:
     * - Email: delivered, opened, clicked, bounced
     * - WhatsApp: sent, delivered, read, failed
     *
     * @param notificationId ID de la notificación (del proveedor)
     * @return Notification con estado actualizado
     * @throws NotificationException si no existe
     */
    Notification getStatus(String notificationId);

    /**
     * Obtiene el tipo de canal que soporta este proveedor.
     *
     * @return Tipo de canal (EMAIL, SMS, WHATSAPP, etc.)
     */
    NotificationChannel getSupportedChannel();

    /**
     * Valida que la configuración del proveedor sea correcta.
     *
     * Verifica:
     * - API keys presentes
     * - Credenciales válidas
     * - Conexión al servicio
     *
     * @return true si está configurado correctamente
     */
    boolean isConfigured();
}
