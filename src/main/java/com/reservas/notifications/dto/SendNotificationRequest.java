package com.reservas.notifications.dto;

import com.reservas.notifications.domain.NotificationChannel;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    private String usuarioId;

    @NotNull(message = "Canal es requerido")
    private NotificationChannel channel;

    @NotBlank(message = "Destinatario es requerido")
    private String recipient;

    private String recipientName;

    @Size(max = 500, message = "El asunto no puede exceder 500 caracteres")
    private String subject;

    @NotBlank(message = "Contenido es requerido")
    private String content;

    private String templateId;
    private java.util.Map<String, Object> templateVariables;
    private String relatedEntityId;
    private String relatedEntityType;
    private java.util.Map<String, String> metadata;
}
