package com.reservas.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para envío de emails usando Resend API
 * Documentación: https://resend.com/docs/api-reference/emails/send-email
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@reservas.com}")
    private String fromEmail;

    @Value("${resend.from.name:Sistema de Reservas}")
    private String fromName;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            log.info("[Resend] Email service inicializado - From: {} <{}>", fromName, fromEmail);
        } else {
            log.warn("[Resend] API key no configurada. Los emails no serán enviados.");
        }
    }

    /**
     * Realiza el POST a la API de Resend.
     * Método protegido para facilitar el testing (stubbing vía spy).
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> doPost(String url, HttpEntity<?> entity) {
        return restTemplate.postForEntity(url, entity, Map.class);
    }

    /**
     * Envía un email simple usando la API de Resend.
     *
     * @param destinatario Email del destinatario
     * @param asunto       Asunto del email
     * @param contenido    Contenido HTML del email
     * @return true si se envió correctamente
     */
    public boolean enviarEmail(String destinatario, String asunto, String contenido) {
        log.info("Enviando email a: {} - Asunto: {}", destinatario, asunto);

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("⚠️ Resend no configurado. Email no enviado.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromName + " <" + fromEmail + ">");
            body.put("to", List.of(destinatario));
            body.put("subject", asunto);
            body.put("html", contenido);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = doPost(RESEND_API_URL, request);

            if (response.getStatusCode().is2xxSuccessful()) {
                String emailId = response.getBody() != null ? (String) response.getBody().get("id") : null;
                log.info("✅ Email enviado exitosamente a: {} - ID: {}", destinatario, emailId);
                return true;
            } else {
                log.error("❌ Error al enviar email. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("❌ Error al enviar email a {}: {}", destinatario, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mantiene compatibilidad con código que usaba templates dinámicos.
     * Con Resend los templates se construyen localmente en HTML.
     *
     * @param destinatario  Email del destinatario
     * @param templateId    ID de referencia del template (informativo)
     * @param templateData  Datos del template
     * @return true si se envió correctamente
     */
    public boolean enviarEmailConTemplate(String destinatario, String templateId, Map<String, Object> templateData) {
        log.info("Enviando email (template ref: {}) a: {}", templateId, destinatario);

        // Con Resend construimos el HTML localmente a partir de los datos del template
        StringBuilder html = new StringBuilder(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">"
        );
        templateData.forEach((k, v) ->
                html.append("<p><strong>").append(k).append(":</strong> ").append(v).append("</p>")
        );
        html.append("</div>");

        return enviarEmail(destinatario, "Notificación", html.toString());
    }

    /**
     * Envía confirmación de registro.
     *
     * @param destinatario Email del nuevo usuario
     * @param nombreUsuario Nombre del usuario
     * @return true si se envió correctamente
     */
    public boolean enviarConfirmacionRegistro(String destinatario, String nombreUsuario) {
        String asunto = "Bienvenido a Cita Click";
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h1 style=\"color: #1E40AF;\">¡Bienvenido %s!</h1>" +
                "<p>Tu cuenta ha sido creada exitosamente en <strong>Cita Click</strong>.</p>" +
                "<p>Ya puedes comenzar a gestionar tus citas y clientes.</p>" +
                "</div>",
                nombreUsuario
        );
        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía recordatorio de cita.
     *
     * @param destinatario   Email del cliente
     * @param nombreCliente  Nombre del cliente
     * @param fechaCita      Fecha de la cita (ej: "Lunes 20 de Enero, 2026")
     * @param horaCita       Hora de la cita (ej: "10:00 AM")
     * @param nombreServicio Nombre del servicio
     * @param nombreNegocio  Nombre del negocio
     * @return true si se envió correctamente
     */
    public boolean enviarRecordatorioCita(String destinatario, String nombreCliente, String fechaCita,
                                          String horaCita, String nombreServicio, String nombreNegocio) {
        String asunto = String.format("Recordatorio de cita - %s", nombreNegocio);
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #1E40AF;\">Recordatorio de Cita</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Te recordamos tu cita próxima:</p>" +
                "<table style=\"width: 100%%; border-collapse: collapse; margin: 20px 0;\">" +
                "<tr style=\"background-color: #F3F4F6;\">" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\"><strong>Servicio</strong></td>" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\">%s</td></tr>" +
                "<tr><td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\"><strong>Fecha</strong></td>" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\">%s</td></tr>" +
                "<tr style=\"background-color: #F3F4F6;\">" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\"><strong>Hora</strong></td>" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\">%s</td></tr>" +
                "<tr><td style=\"padding: 10px 16px;\"><strong>Lugar</strong></td>" +
                "<td style=\"padding: 10px 16px;\">%s</td></tr>" +
                "</table>" +
                "<p>¡Te esperamos!</p>" +
                "</div>",
                nombreCliente, nombreServicio, fechaCita, horaCita, nombreNegocio
        );
        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Sobrecarga del método anterior para mantener compatibilidad.
     * @deprecated Usar el método con fechaCita y horaCita separadas
     */
    @Deprecated
    public boolean enviarRecordatorioCita(String destinatario, String nombreCliente, String fechaHora,
                                          String nombreServicio, String nombreNegocio) {
        return enviarRecordatorioCita(destinatario, nombreCliente, fechaHora, "", nombreServicio, nombreNegocio);
    }

    /**
     * Envía confirmación de cita.
     *
     * @param destinatario   Email del cliente
     * @param nombreCliente  Nombre del cliente
     * @param fechaHora      Fecha y hora de la cita
     * @param nombreServicio Nombre del servicio
     * @return true si se envió correctamente
     */
    public boolean enviarConfirmacionCita(String destinatario, String nombreCliente,
                                          String fechaHora, String nombreServicio) {
        String asunto = "Confirmación de cita - Cita Click";
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #1E40AF;\">Cita Confirmada ✓</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Tu cita ha sido confirmada exitosamente.</p>" +
                "<table style=\"width: 100%%; border-collapse: collapse; margin: 20px 0;\">" +
                "<tr style=\"background-color: #F3F4F6;\">" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\"><strong>Servicio</strong></td>" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\">%s</td></tr>" +
                "<tr><td style=\"padding: 10px 16px;\"><strong>Fecha y hora</strong></td>" +
                "<td style=\"padding: 10px 16px;\">%s</td></tr>" +
                "</table>" +
                "<p>Si necesitas hacer algún cambio, por favor contáctanos.</p>" +
                "</div>",
                nombreCliente, nombreServicio, fechaHora
        );
        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía email de invitación a un nuevo usuario del negocio.
     *
     * @param destinatario     Email del nuevo usuario
     * @param nombreUsuario    Nombre del usuario
     * @param nombreNegocio    Nombre del negocio
     * @param passwordTemporal Contraseña temporal asignada
     * @return true si se envió correctamente
     */
    public boolean enviarEmailInvitacionUsuario(String destinatario, String nombreUsuario,
                                                String nombreNegocio, String passwordTemporal) {
        String asunto = String.format("Invitación a unirte a %s en Cita Click", nombreNegocio);
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #1E40AF;\">¡Has sido invitado!</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Has sido invitado a unirte a <strong>%s</strong> en Cita Click.</p>" +
                "<h3 style=\"color: #374151;\">Tus credenciales de acceso:</h3>" +
                "<table style=\"width: 100%%; border-collapse: collapse; margin: 20px 0;\">" +
                "<tr style=\"background-color: #F3F4F6;\">" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\"><strong>Email</strong></td>" +
                "<td style=\"padding: 10px 16px; border-bottom: 1px solid #E5E7EB;\">%s</td></tr>" +
                "<tr><td style=\"padding: 10px 16px;\"><strong>Contraseña temporal</strong></td>" +
                "<td style=\"padding: 10px 16px; font-family: monospace;\">%s</td></tr>" +
                "</table>" +
                "<p style=\"color: #DC2626;\"><strong>Importante:</strong> Cambia tu contraseña al iniciar sesión por primera vez.</p>" +
                "<p>¡Bienvenido al equipo!</p>" +
                "</div>",
                nombreUsuario, nombreNegocio, destinatario, passwordTemporal
        );
        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía email de verificación de cuenta.
     *
     * @param destinatario    Email del destinatario
     * @param nombreUsuario   Nombre del usuario
     * @param verificationUrl URL de verificación
     * @return true si se envió correctamente
     */
    public boolean enviarEmailVerificacion(String destinatario, String nombreUsuario, String verificationUrl) {
        String asunto = "Verifica tu cuenta en Cita Click";
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #1E40AF;\">Verifica tu cuenta</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Para completar tu registro, verifica tu dirección de correo electrónico:</p>" +
                "<p style=\"text-align: center; margin: 30px 0;\">" +
                "<a href=\"%s\" style=\"background-color: #1E40AF; color: white; padding: 14px 28px; " +
                "text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">" +
                "Verificar mi cuenta</a></p>" +
                "<p>O copia este enlace en tu navegador:</p>" +
                "<p style=\"word-break: break-all; color: #6B7280; font-size: 13px;\">%s</p>" +
                "<p style=\"color: #9CA3AF; font-size: 12px; margin-top: 30px;\">" +
                "Este enlace expirará en 24 horas. " +
                "Si no solicitaste esta verificación, puedes ignorar este email.</p>" +
                "</div>",
                nombreUsuario, verificationUrl, verificationUrl
        );
        return enviarEmail(destinatario, asunto, contenido);
    }
}
