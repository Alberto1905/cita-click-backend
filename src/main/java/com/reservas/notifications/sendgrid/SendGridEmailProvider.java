package com.reservas.notifications.sendgrid;

import com.reservas.exception.NotificationException;
import com.reservas.notifications.domain.Notification;
import com.reservas.notifications.domain.NotificationChannel;
import com.reservas.notifications.domain.NotificationResult;
import com.reservas.notifications.dto.SendNotificationRequest;
import com.reservas.notifications.provider.NotificationProvider;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementación de NotificationProvider usando SendGrid para Email.
 *
 * IMPORTANTE:
 * - Requiere API key de SendGrid
 * - Soporta templates dinámicos
 * - Soporta personalización con variables
 * - Soporta batch sending (hasta 1000 por request)
 */
@Slf4j
@Service("sendGridEmailProvider")
public class SendGridEmailProvider implements NotificationProvider {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Cita Click}")
    private String fromName;

    private SendGrid sendGrid;
    private boolean configured = false;

    @PostConstruct
    public void init() {
        try {
            if (sendGridApiKey != null && !sendGridApiKey.isBlank()) {
                sendGrid = new SendGrid(sendGridApiKey);
                configured = true;
                log.info("[SendGrid]  Inicializado correctamente - From: {} <{}>", fromName, fromEmail);
            } else {
                log.warn("[SendGrid]  No configurado - API key faltante");
            }
        } catch (Exception e) {
            log.error("[SendGrid]  Error en inicialización: {}", e.getMessage(), e);
            configured = false;
        }
    }

    @Override
    public NotificationResult send(SendNotificationRequest request) {
        if (!configured) {
            throw new NotificationException(
                    "SendGrid no está configurado",
                    "SENDGRID_NOT_CONFIGURED"
            );
        }

        try {
            log.info("[SendGrid] Enviando email a: {}", request.getRecipient());

            Email from = new Email(fromEmail, fromName);
            Email to = new Email(request.getRecipient(), request.getRecipientName());

            Mail mail;

            // Si hay templateId, usar template
            if (request.getTemplateId() != null) {
                mail = new Mail();
                mail.setFrom(from);
                mail.setTemplateId(request.getTemplateId());

                Personalization personalization = new Personalization();
                personalization.addTo(to);

                // Agregar variables del template
                if (request.getTemplateVariables() != null) {
                    for (Map.Entry<String, Object> entry : request.getTemplateVariables().entrySet()) {
                        personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
                    }
                }

                mail.addPersonalization(personalization);

            } else {
                // Email sin template
                String subject = request.getSubject() != null ? request.getSubject() : "Notificación";
                Content content = new Content("text/html", request.getContent());
                mail = new Mail(from, subject, to, content);
            }

            // Enviar
            Request sendGridRequest = new Request();
            sendGridRequest.setMethod(Method.POST);
            sendGridRequest.setEndpoint("mail/send");
            sendGridRequest.setBody(mail.build());

            Response response = sendGrid.api(sendGridRequest);

            boolean success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;

            if (success) {
                // SendGrid devuelve el Message ID en el header X-Message-Id
                String messageId = response.getHeaders().get("X-Message-Id");

                log.info("[SendGrid]  Email enviado - Status: {} - MessageId: {}",
                        response.getStatusCode(), messageId);

                return NotificationResult.builder()
                        .success(true)
                        .providerId(messageId)
                        .recipient(request.getRecipient())
                        .channel(NotificationChannel.EMAIL)
                        .message("Email enviado exitosamente")
                        .sentAt(LocalDateTime.now())
                        .build();

            } else {
                log.error("[SendGrid]  Error enviando email - Status: {} - Body: {}",
                        response.getStatusCode(), response.getBody());

                return NotificationResult.builder()
                        .success(false)
                        .recipient(request.getRecipient())
                        .channel(NotificationChannel.EMAIL)
                        .errorCode(String.valueOf(response.getStatusCode()))
                        .errorMessage(response.getBody())
                        .build();
            }

        } catch (IOException e) {
            log.error("[SendGrid]  Error IO enviando email: {}", e.getMessage(), e);

            throw new NotificationException(
                    "Error de comunicación con SendGrid: " + e.getMessage(),
                    "SENDGRID_IO_ERROR"
            );

        } catch (Exception e) {
            log.error("[SendGrid]  Error inesperado: {}", e.getMessage(), e);

            throw new NotificationException(
                    "Error al enviar email: " + e.getMessage(),
                    "SENDGRID_SEND_ERROR"
            );
        }
    }

    @Override
    public List<NotificationResult> sendBatch(List<SendNotificationRequest> requests) {
        // SendGrid soporta batch de hasta 1000 destinatarios
        // Para simplificar, enviamos uno por uno
        // En producción, optimizar agrupando en batches reales

        List<NotificationResult> results = new ArrayList<>();

        for (SendNotificationRequest request : requests) {
            try {
                NotificationResult result = send(request);
                results.add(result);
            } catch (Exception e) {
                log.error("[SendGrid]  Error en batch para {}: {}",
                        request.getRecipient(), e.getMessage());

                results.add(NotificationResult.builder()
                        .success(false)
                        .recipient(request.getRecipient())
                        .channel(NotificationChannel.EMAIL)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        log.info("[SendGrid] Batch completado - Total: {} - Exitosos: {}",
                results.size(),
                results.stream().filter(NotificationResult::getSuccess).count());

        return results;
    }

    @Override
    public Notification getStatus(String notificationId) {
        // SendGrid requiere configuración de Event Webhook para tracking
        // El estado se obtiene vía webhooks, no mediante polling
        // Por ahora, devolvemos placeholder

        log.warn("[SendGrid] getStatus no implementado - usar webhooks para tracking");

        return Notification.builder()
                .id(notificationId)
                .channel(NotificationChannel.EMAIL)
                .providerId(notificationId)
                .status(Notification.NotificationStatus.SENT)
                .build();
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }
}
