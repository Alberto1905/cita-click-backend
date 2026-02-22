package com.reservas.notifications.resend;

import com.reservas.exception.NotificationException;
import com.reservas.notifications.domain.Notification;
import com.reservas.notifications.domain.NotificationChannel;
import com.reservas.notifications.domain.NotificationResult;
import com.reservas.notifications.dto.SendNotificationRequest;
import com.reservas.notifications.provider.NotificationProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de NotificationProvider usando Resend para Email.
 *
 * Utiliza la API REST de Resend directamente:
 * - POST https://api.resend.com/emails
 * - Authorization: Bearer {API_KEY}
 *
 * Documentación: https://resend.com/docs/api-reference/emails/send-email
 */
@Slf4j
@Service("resendEmailProvider")
public class ResendEmailProvider implements NotificationProvider {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@citaclick.com.mx}")
    private String fromEmail;

    @Value("${resend.from.name:Cita Click}")
    private String fromName;

    private RestTemplate restTemplate;
    private boolean configured = false;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            configured = true;
            log.info("[Resend] Provider inicializado correctamente - From: {} <{}>", fromName, fromEmail);
        } else {
            log.warn("[Resend] Provider no configurado - API key faltante");
        }
    }

    @Override
    public NotificationResult send(SendNotificationRequest request) {
        if (!configured) {
            throw new NotificationException(
                    "Resend no está configurado",
                    "RESEND_NOT_CONFIGURED"
            );
        }

        try {
            log.info("[Resend] Enviando email a: {}", request.getRecipient());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String subject = request.getSubject() != null ? request.getSubject() : "Notificación";
            String htmlContent = request.getContent() != null ? request.getContent() : "";

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromName + " <" + fromEmail + ">");
            body.put("to", List.of(request.getRecipient()));
            body.put("subject", subject);
            body.put("html", htmlContent);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(RESEND_API_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = response.getBody() != null ? (String) response.getBody().get("id") : null;
                log.info("[Resend] ✅ Email enviado - MessageId: {}", messageId);

                return NotificationResult.builder()
                        .success(true)
                        .providerId(messageId)
                        .recipient(request.getRecipient())
                        .channel(NotificationChannel.EMAIL)
                        .message("Email enviado exitosamente")
                        .sentAt(LocalDateTime.now())
                        .build();
            } else {
                log.error("[Resend] ❌ Error enviando email - Status: {}", response.getStatusCode());

                return NotificationResult.builder()
                        .success(false)
                        .recipient(request.getRecipient())
                        .channel(NotificationChannel.EMAIL)
                        .errorCode(response.getStatusCode().toString())
                        .errorMessage("Error HTTP al enviar email: " + response.getStatusCode())
                        .build();
            }

        } catch (RestClientException e) {
            log.error("[Resend] ❌ Error HTTP enviando email a {}: {}", request.getRecipient(), e.getMessage(), e);

            throw new NotificationException(
                    "Error de comunicación con Resend: " + e.getMessage(),
                    "RESEND_HTTP_ERROR"
            );
        } catch (Exception e) {
            log.error("[Resend] ❌ Error inesperado: {}", e.getMessage(), e);

            throw new NotificationException(
                    "Error al enviar email: " + e.getMessage(),
                    "RESEND_SEND_ERROR"
            );
        }
    }

    @Override
    public List<NotificationResult> sendBatch(List<SendNotificationRequest> requests) {
        List<NotificationResult> results = new ArrayList<>();

        for (SendNotificationRequest request : requests) {
            try {
                NotificationResult result = send(request);
                results.add(result);
            } catch (Exception e) {
                log.error("[Resend] Error en batch para {}: {}", request.getRecipient(), e.getMessage());

                results.add(NotificationResult.builder()
                        .success(false)
                        .recipient(request.getRecipient())
                        .channel(NotificationChannel.EMAIL)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        log.info("[Resend] Batch completado - Total: {} - Exitosos: {}",
                results.size(),
                results.stream().filter(NotificationResult::getSuccess).count());

        return results;
    }

    @Override
    public Notification getStatus(String notificationId) {
        // Resend proporciona tracking vía webhooks, no mediante polling
        log.warn("[Resend] getStatus no implementado - usar webhooks para tracking de entregas");

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
