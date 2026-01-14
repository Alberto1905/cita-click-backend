package com.reservas.notifications.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resultado del envío de una notificación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    private Boolean success;
    private String providerId;
    private String recipient;
    private NotificationChannel channel;
    private String message;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime sentAt;
}
