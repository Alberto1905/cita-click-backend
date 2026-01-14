package com.reservas.notifications.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa una notificación en el dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String id;
    private NotificationChannel channel;
    private String providerId;
    private String recipient;
    private String subject;
    private String content;
    private NotificationStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private String errorMessage;
    private java.util.Map<String, String> metadata;

    public enum NotificationStatus {
        PENDING,
        QUEUED,
        SENT,
        DELIVERED,
        READ,
        FAILED,
        BOUNCED
    }
}
