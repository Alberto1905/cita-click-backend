package com.reservas.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Servicio para envío de SMS y WhatsApp usando Twilio
 * FUNCIONAL - Solo requiere credenciales configuradas
 */
@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;

    @Value("${twilio.whatsapp.number:}")
    private String twilioWhatsAppNumber;

    @PostConstruct
    public void init() {
        if (twilioAccountSid != null && !twilioAccountSid.isBlank() &&
            twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("✅ Twilio inicializado correctamente");
        } else {
            log.warn("⚠️ Twilio no configurado. SMS/WhatsApp deshabilitados.");
        }
    }

    /**
     * Envía un SMS a un número de teléfono
     * @param telefono Número de teléfono destino (formato: +52XXXXXXXXXX)
     * @param mensaje Contenido del mensaje
     * @return true si se envió correctamente
     */
    public boolean enviarSms(String telefono, String mensaje) {
        log.info("Enviando SMS a: {} - Mensaje: {}", telefono, mensaje);

        if (twilioAccountSid == null || twilioAccountSid.isBlank()) {
            log.warn("⚠️ Twilio no configurado. SMS no enviado.");
            return false;
        }

        try {
            Message message = Message.creator(
                new PhoneNumber(telefono),
                new PhoneNumber(twilioPhoneNumber),
                mensaje
            ).create();

            log.info("✅ SMS enviado exitosamente. SID: {}", message.getSid());
            return true;
        } catch (Exception e) {
            log.error("❌ Error al enviar SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envía un mensaje de WhatsApp
     * @param telefono Número de teléfono destino (formato: +52XXXXXXXXXX)
     * @param mensaje Contenido del mensaje
     * @return true si se envió correctamente
     */
    public boolean enviarWhatsApp(String telefono, String mensaje) {
        log.info("Enviando WhatsApp a: {} - Mensaje: {}", telefono, mensaje);

        if (twilioAccountSid == null || twilioAccountSid.isBlank()) {
            log.warn("⚠️ Twilio no configurado. WhatsApp no enviado.");
            return false;
        }

        try {
            // El número debe tener prefijo "whatsapp:"
            String whatsappFrom = twilioWhatsAppNumber != null && !twilioWhatsAppNumber.isBlank()
                ? twilioWhatsAppNumber
                : "whatsapp:" + twilioPhoneNumber;

            Message message = Message.creator(
                new PhoneNumber("whatsapp:" + telefono),
                new PhoneNumber(whatsappFrom),
                mensaje
            ).create();

            log.info("✅ WhatsApp enviado exitosamente. SID: {}", message.getSid());
            return true;
        } catch (Exception e) {
            log.error("❌ Error al enviar WhatsApp: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envía un recordatorio de cita por SMS
     * @param telefono Número de teléfono del cliente
     * @param nombreCliente Nombre del cliente
     * @param fechaHora Fecha y hora de la cita
     * @param nombreServicio Nombre del servicio
     * @return true si se envió correctamente
     */
    public boolean enviarRecordatorioCita(String telefono, String nombreCliente, String fechaHora, String nombreServicio) {
        String mensaje = String.format(
                "Hola %s, te recordamos tu cita de %s para el %s. ¡Te esperamos!",
                nombreCliente,
                nombreServicio,
                fechaHora
        );

        return enviarSms(telefono, mensaje);
    }
}
