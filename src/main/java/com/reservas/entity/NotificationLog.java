package com.reservas.entity;

import com.reservas.notifications.domain.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que registra todas las notificaciones enviadas.
 *
 * Mantiene un historial completo de:
 * - Emails enviados (SendGrid)
 * - WhatsApp enviados (Twilio)
 * - SMS enviados
 *
 * IMPORTANTE:
 * - Guardar logs permite auditoría y debugging
 * - El provider_message_id se usa para tracking
 * - Los webhooks actualizan el estado (delivered, read, bounced, etc.)
 */
@Entity
@Table(name = "tbl_notification_logs", schema = "ccdiad", indexes = {
    @Index(name = "idx_notification_logs_provider_msg_id", columnList = "provider_message_id"),
    @Index(name = "idx_notification_logs_usuario_id", columnList = "usuario_id"),
    @Index(name = "idx_notification_logs_channel", columnList = "channel"),
    @Index(name = "idx_notification_logs_status", columnList = "status"),
    @Index(name = "idx_notification_logs_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Usuario del SaaS que envía la notificación.
     * Puede ser null si es una notificación del sistema.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * Canal utilizado para enviar la notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    /**
     * Proveedor utilizado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private NotificationProvider provider;

    /**
     * ID del mensaje en el proveedor (SID de Twilio, MessageID de SendGrid, etc.).
     * Se usa para tracking y consultar estado.
     */
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    /**
     * Destinatario de la notificación.
     * - Para EMAIL: dirección de correo
     * - Para WHATSAPP/SMS: número de teléfono con código de país (+521234567890)
     */
    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    /**
     * Nombre del destinatario (si está disponible).
     */
    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    /**
     * Asunto (solo para emails).
     */
    @Column(name = "subject", length = 500)
    private String subject;

    /**
     * Contenido del mensaje.
     * Para emails puede ser HTML.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * ID del template usado (si aplica).
     * SendGrid: d-xxxxx
     * Twilio: HXxxxxx
     */
    @Column(name = "template_id", length = 100)
    private String templateId;

    /**
     * Variables del template (JSON).
     */
    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    /**
     * Estado actual de la notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    /**
     * Tipo de notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 30)
    private NotificationType notificationType;

    /**
     * ID de la entidad relacionada (cita_id, payment_id, etc.).
     */
    @Column(name = "related_entity_id", length = 100)
    private String relatedEntityId;

    /**
     * Tipo de entidad relacionada.
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /**
     * Indica si fue enviado exitosamente al proveedor.
     */
    @Column(name = "sent_successfully", nullable = false)
    private Boolean sentSuccessfully;

    /**
     * Mensaje de error (si hubo fallo).
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Código de error del proveedor.
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /**
     * Número de intentos de envío.
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    /**
     * Fecha de envío exitoso.
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Fecha de entrega (delivery).
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Fecha de lectura/apertura.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Fecha de click (para emails).
     */
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    /**
     * Fecha de bounce/rebote.
     */
    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;

    /**
     * Razón del bounce.
     */
    @Column(name = "bounce_reason", length = 255)
    private String bounceReason;

    /**
     * Metadatos adicionales (JSON).
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Proveedores de notificaciones
     */
    public enum NotificationProvider {
        SENDGRID,
        TWILIO,
        AWS_SES,
        FIREBASE,
        INTERNAL
    }

    /**
     * Estados de notificación
     */
    public enum NotificationStatus {
        /**
         * Pendiente de envío
         */
        PENDING,

        /**
         * En cola para envío
         */
        QUEUED,

        /**
         * Enviado al proveedor
         */
        SENT,

        /**
         * Entregado al destinatario
         */
        DELIVERED,

        /**
         * Leído/Abierto
         */
        READ,

        /**
         * Click realizado (emails)
         */
        CLICKED,

        /**
         * Falló el envío
         */
        FAILED,

        /**
         * Rebotó (bounce)
         */
        BOUNCED,

        /**
         * Usuario se dio de baja
         */
        UNSUBSCRIBED
    }

    /**
     * Tipos de notificación según propósito
     */
    public enum NotificationType {
        APPOINTMENT_CONFIRMATION,
        APPOINTMENT_REMINDER,
        APPOINTMENT_CANCELLATION,
        PAYMENT_CONFIRMATION,
        PAYMENT_FAILED,
        INVOICE_PAID,
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_CANCELED,
        ONBOARDING_WELCOME,
        PASSWORD_RESET,
        VERIFICATION_CODE,
        MARKETING,
        SYSTEM_ALERT,
        CUSTOM
    }
}
