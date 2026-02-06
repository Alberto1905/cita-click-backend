package com.reservas.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Servicio para envío de emails usando SendGrid
 * FUNCIONAL - Solo requiere API Key configurada
 */
@Service
@Slf4j
public class EmailService {

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:noreply@reservas.com}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Sistema de Reservas}")
    private String fromName;

    @Value("${sendgrid.template.verification:d-feb3b116023c461999dd46f216bae714}")
    private String verificationTemplateId;

    @Value("${sendgrid.template.reminder:}")
    private String reminderTemplateId;

    /**
     * Envía un email usando un Dynamic Template de SendGrid
     * @param destinatario Email del destinatario
     * @param templateId ID del template dinámico de SendGrid
     * @param templateData Map con las variables del template
     * @return true si se envió correctamente
     */
    public boolean enviarEmailConTemplate(String destinatario, String templateId, Map<String, Object> templateData) {
        log.info("Enviando email con template {} a: {}", templateId, destinatario);

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.warn("⚠️ SendGrid no configurado. Email no enviado.");
            return false;
        }

        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(destinatario);

            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setTemplateId(templateId);

            Personalization personalization = new Personalization();
            personalization.addTo(to);

            // Agregar variables dinámicas al template
            templateData.forEach(personalization::addDynamicTemplateData);

            mail.addPersonalization(personalization);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("✅ Email con template enviado exitosamente a: {}", destinatario);
                return true;
            } else {
                log.error("❌ Error al enviar email con template. Status: {} - Body: {}",
                    response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("❌ Error al enviar email con template: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envía un email simple
     * @param destinatario Email del destinatario
     * @param asunto Asunto del email
     * @param contenido Contenido del email (HTML o texto plano)
     * @return true si se envió correctamente
     */
    public boolean enviarEmail(String destinatario, String asunto, String contenido) {
        log.info("Enviando email a: {} - Asunto: {}", destinatario, asunto);

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.warn("⚠️ SendGrid no configurado. Email no enviado.");
            return false;
        }

        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(destinatario);
            Content content = new Content("text/html", contenido);
            Mail mail = new Mail(from, asunto, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("✅ Email enviado exitosamente a: {}", destinatario);
                return true;
            } else {
                log.error("❌ Error al enviar email. Status: {} - Body: {}",
                    response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("❌ Error al enviar email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envía confirmación de registro
     * @param destinatario Email del nuevo usuario
     * @param nombreUsuario Nombre del usuario
     * @return true si se envió correctamente
     */
    public boolean enviarConfirmacionRegistro(String destinatario, String nombreUsuario) {
        String asunto = "Bienvenido a Sistema de Reservas";
        String contenido = String.format(
                "<h1>¡Bienvenido %s!</h1>" +
                "<p>Tu cuenta ha sido creada exitosamente.</p>" +
                "<p>Ya puedes comenzar a gestionar tus citas y clientes.</p>",
                nombreUsuario
        );

        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía recordatorio de cita por email usando Dynamic Template de SendGrid
     * @param destinatario Email del cliente
     * @param nombreCliente Nombre del cliente
     * @param fechaCita Fecha de la cita formateada (ej: "Lunes 20 de Enero, 2026")
     * @param horaCita Hora de la cita formateada (ej: "10:00 AM")
     * @param nombreServicio Nombre del servicio
     * @param nombreNegocio Nombre del negocio
     * @return true si se envió correctamente
     */
    public boolean enviarRecordatorioCita(String destinatario, String nombreCliente, String fechaCita,
                                         String horaCita, String nombreServicio, String nombreNegocio) {
        // Si no hay template configurado, usar el método antiguo
        if (reminderTemplateId == null || reminderTemplateId.isBlank()) {
            log.warn("⚠️ Template de recordatorio no configurado. Usando email simple.");
            String asunto = String.format("Recordatorio de cita - %s", nombreNegocio);
            String contenido = String.format(
                    "<h2>Recordatorio de Cita</h2>" +
                    "<p>Hola %s,</p>" +
                    "<p>Te recordamos tu cita:</p>" +
                    "<ul>" +
                    "<li><strong>Servicio:</strong> %s</li>" +
                    "<li><strong>Fecha:</strong> %s</li>" +
                    "<li><strong>Hora:</strong> %s</li>" +
                    "<li><strong>Lugar:</strong> %s</li>" +
                    "</ul>" +
                    "<p>¡Te esperamos!</p>",
                    nombreCliente,
                    nombreServicio,
                    fechaCita,
                    horaCita,
                    nombreNegocio
            );
            return enviarEmail(destinatario, asunto, contenido);
        }

        // Usar Dynamic Template
        Map<String, Object> templateData = Map.of(
            "nombreCliente", nombreCliente,
            "nombreServicio", nombreServicio,
            "fechaCita", fechaCita,
            "horaCita", horaCita,
            "nombreNegocio", nombreNegocio
        );

        return enviarEmailConTemplate(destinatario, reminderTemplateId, templateData);
    }

    /**
     * Sobrecarga del método anterior para mantener compatibilidad
     * @deprecated Usar el método con fechaCita y horaCita separadas
     */
    @Deprecated
    public boolean enviarRecordatorioCita(String destinatario, String nombreCliente, String fechaHora,
                                         String nombreServicio, String nombreNegocio) {
        // Convertir fechaHora a formato separado (implementación simple)
        return enviarRecordatorioCita(destinatario, nombreCliente, fechaHora, "", nombreServicio, nombreNegocio);
    }

    /**
     * Envía confirmación de cita
     * @param destinatario Email del cliente
     * @param nombreCliente Nombre del cliente
     * @param fechaHora Fecha y hora de la cita
     * @param nombreServicio Nombre del servicio
     * @return true si se envió correctamente
     */
    public boolean enviarConfirmacionCita(String destinatario, String nombreCliente, String fechaHora, String nombreServicio) {
        String asunto = "Confirmación de cita";
        String contenido = String.format(
                "<h2>Cita Confirmada</h2>" +
                "<p>Hola %s,</p>" +
                "<p>Tu cita ha sido confirmada:</p>" +
                "<ul>" +
                "<li><strong>Servicio:</strong> %s</li>" +
                "<li><strong>Fecha y hora:</strong> %s</li>" +
                "</ul>" +
                "<p>Si necesitas hacer algún cambio, por favor contáctanos.</p>",
                nombreCliente,
                nombreServicio,
                fechaHora
        );

        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía email de invitación a un nuevo usuario
     * @param destinatario Email del nuevo usuario
     * @param nombreUsuario Nombre del usuario
     * @param nombreNegocio Nombre del negocio
     * @param passwordTemporal Password temporal
     * @return true si se envió correctamente
     */
    public boolean enviarEmailInvitacionUsuario(String destinatario, String nombreUsuario,
                                                String nombreNegocio, String passwordTemporal) {
        String asunto = String.format("Invitación a %s - Sistema de Reservas", nombreNegocio);
        String contenido = String.format(
                "<h2>¡Has sido invitado!</h2>" +
                "<p>Hola %s,</p>" +
                "<p>Has sido invitado a unirte a <strong>%s</strong> en el Sistema de Reservas.</p>" +
                "<h3>Tus credenciales de acceso:</h3>" +
                "<ul>" +
                "<li><strong>Email:</strong> %s</li>" +
                "<li><strong>Contraseña temporal:</strong> %s</li>" +
                "</ul>" +
                "<p><strong>Importante:</strong> Por seguridad, te recomendamos cambiar tu contraseña después del primer inicio de sesión.</p>" +
                "<p>¡Bienvenido al equipo!</p>",
                nombreUsuario,
                nombreNegocio,
                destinatario,
                passwordTemporal
        );

        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía email de verificación usando el Dynamic Template de SendGrid
     * @param destinatario Email del destinatario
     * @param nombreUsuario Nombre del usuario
     * @param verificationUrl URL de verificación completa
     * @return true si se envió correctamente
     */
    public boolean enviarEmailVerificacion(String destinatario, String nombreUsuario, String verificationUrl) {
        Map<String, Object> templateData = Map.of(
            "nombre", nombreUsuario,
            "verificationUrl", verificationUrl
        );

        return enviarEmailConTemplate(destinatario, verificationTemplateId, templateData);
    }
}
