package com.reservas.controller;

import com.reservas.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para recibir webhooks de Twilio (WhatsApp y SMS)
 * IMPORTANTE: Este endpoint NO debe tener autenticación JWT
 *
 * Twilio enviará notificaciones de estado de mensajes aquí:
 * - Mensaje enviado
 * - Mensaje entregado
 * - Mensaje leído
 * - Mensaje fallido
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/twilio")
@RequiredArgsConstructor
@Hidden
public class TwilioWebhookController {

    private final NotificationService notificationService;

    /**
     * POST /api/webhooks/twilio/whatsapp
     * Recibe eventos de estado de mensajes de WhatsApp desde Twilio
     *
     * IMPORTANTE: Este endpoint debe estar en WebSecurityConfig.permitAll()
     */
    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleWhatsAppWebhook(@RequestParam Map<String, String> params) {
        log.info("[Twilio Webhook] WhatsApp event recibido");

        try {
            // Parámetros comunes de Twilio
            String messageSid = params.get("MessageSid");
            String messageStatus = params.get("MessageStatus");
            String from = params.get("From");
            String to = params.get("To");
            String body = params.get("Body");

            log.info("[Twilio Webhook] MessageSid: {} - Status: {} - From: {} - To: {}",
                    messageSid, messageStatus, from, to, body);

            // Aquí puedes actualizar el estado de la notificación en tu base de datos
            // Por ejemplo: notificationService.updateStatus(messageSid, messageStatus);

            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");

        } catch (Exception e) {
            log.error("[Twilio Webhook] Error procesando webhook de WhatsApp", e);
            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
        }
    }

    /**
     * POST /api/webhooks/twilio/sms
     * Recibe eventos de estado de mensajes SMS desde Twilio
     */
    @PostMapping(value = "/sms", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleSmsWebhook(@RequestParam Map<String, String> params) {
        log.info("[Twilio Webhook] SMS event recibido");

        try {
            String messageSid = params.get("MessageSid");
            String messageStatus = params.get("MessageStatus");
            String from = params.get("From");
            String to = params.get("To");

            log.info("[Twilio Webhook] MessageSid: {} - Status: {} - From: {} - To: {}",
                    messageSid, messageStatus, from, to);

            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");

        } catch (Exception e) {
            log.error("[Twilio Webhook] Error procesando webhook de SMS", e);
            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
        }
    }

    /**
     * POST /api/webhooks/twilio/status
     * Callback genérico para actualizaciones de estado de mensajes
     */
    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleStatusCallback(@RequestParam Map<String, String> params) {
        log.info("[Twilio Webhook] Status callback recibido");

        try {
            String messageSid = params.get("MessageSid");
            String messageStatus = params.get("MessageStatus");
            String errorCode = params.get("ErrorCode");
            String errorMessage = params.get("ErrorMessage");

            log.info("[Twilio Webhook] MessageSid: {} - Status: {}", messageSid, messageStatus);

            if (errorCode != null) {
                log.error("[Twilio Webhook] Error - Code: {} - Message: {}", errorCode, errorMessage);
            }

            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");

        } catch (Exception e) {
            log.error("[Twilio Webhook] Error procesando status callback", e);
            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
        }
    }
}
