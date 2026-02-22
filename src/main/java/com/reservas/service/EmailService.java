package com.reservas.service;

import com.reservas.entity.PlantillaEmailConfig;
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
 * Servicio para envío de emails usando Resend API.
 * Documentación: https://resend.com/docs/api-reference/emails/send-email
 *
 * Soporta dos modos de envío:
 *  - HTML inline:  enviarEmail() — para emails sin template configurado
 *  - Template ID:  enviarConTemplateResend() — usa templates creados en Resend Dashboard
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    // ──────────────────────────────────────────
    // Credenciales y remitente
    // ──────────────────────────────────────────

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@reservas.com}")
    private String fromEmail;

    @Value("${resend.from.name:Sistema de Reservas}")
    private String fromName;

    // ──────────────────────────────────────────
    // Template IDs (creados en Resend Dashboard)
    // ──────────────────────────────────────────

    /** Template: verificación de correo electrónico */
    @Value("${resend.templates.verificacion-correo:}")
    private String templateVerificacion;

    /** Template: recordatorio de cita — diseño Clásico */
    @Value("${resend.templates.recordatorio-clasico:}")
    private String templateRecordatorioClasico;

    /** Template: recordatorio de cita — diseño Moderno */
    @Value("${resend.templates.recordatorio-moderno:}")
    private String templateRecordatorioModerno;

    /** Template: recordatorio de cita — diseño Minimalista */
    @Value("${resend.templates.recordatorio-minimalista:}")
    private String templateRecordatorioMinimalista;

    // ──────────────────────────────────────────

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
     * Método protegido para facilitar el testing (stubbing vía subclase).
     */
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map> doPost(String url, HttpEntity<?> entity) {
        return restTemplate.postForEntity(url, entity, Map.class);
    }

    // ══════════════════════════════════════════════════════════════════
    // ENVÍO CON TEMPLATE DE RESEND
    // ══════════════════════════════════════════════════════════════════

    /**
     * Envía un email usando un template creado en el Dashboard de Resend.
     * Las variables reemplazan los placeholders {@code {{variable}}} del template HTML.
     *
     * Ref API: POST https://api.resend.com/emails
     *         body: { template_id, variables, from, to, subject }
     *
     * @param destinatario Email del destinatario
     * @param asunto       Asunto del email (sobreescribe el asunto del template si aplica)
     * @param templateId   ID del template configurado en Resend Dashboard
     * @param variables    Mapa de variables para reemplazar en el template
     * @return true si se envió correctamente
     */
    private boolean enviarConTemplateResend(String destinatario, String asunto,
                                            String templateId, Map<String, Object> variables) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("⚠️ Resend no configurado. Email no enviado a: {}", destinatario);
            return false;
        }
        if (templateId == null || templateId.isBlank()) {
            log.warn("⚠️ Template ID no configurado para el asunto '{}'. Verifica la variable de entorno.", asunto);
            return false;
        }

        log.info("[Resend] Enviando email con template '{}' a: {}", templateId, destinatario);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromName + " <" + fromEmail + ">");
            body.put("to", List.of(destinatario));
            body.put("subject", asunto);
            body.put("template_id", templateId);
            body.put("variables", variables);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = doPost(RESEND_API_URL, request);

            if (response.getStatusCode().is2xxSuccessful()) {
                String emailId = response.getBody() != null ? (String) response.getBody().get("id") : null;
                log.info("✅ Email enviado a: {} | template: {} | id: {}", destinatario, templateId, emailId);
                return true;
            } else {
                log.error("❌ Error al enviar email. Status: {} | template: {}", response.getStatusCode(), templateId);
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
     * Devuelve el template ID de recordatorio según el diseño de la plantilla del negocio.
     */
    private String resolverTemplateRecordatorio(PlantillaEmailConfig.TipoDiseno diseno) {
        if (diseno == null) return templateRecordatorioClasico;
        return switch (diseno) {
            case MODERNO      -> templateRecordatorioModerno;
            case MINIMALISTA  -> templateRecordatorioMinimalista;
            default           -> templateRecordatorioClasico;
        };
    }

    // ══════════════════════════════════════════════════════════════════
    // ENVÍO CON HTML INLINE (genérico / fallback)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Envía un email simple con HTML inline usando la API de Resend.
     * Úsalo cuando no haya un template configurado o para emails one-off.
     *
     * @param destinatario Email del destinatario
     * @param asunto       Asunto del email
     * @param contenido    Contenido HTML del email
     * @return true si se envió correctamente
     */
    public boolean enviarEmail(String destinatario, String asunto, String contenido) {
        log.info("[Resend] Enviando email a: {} | Asunto: {}", destinatario, asunto);

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
                log.info("✅ Email enviado a: {} | id: {}", destinatario, emailId);
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
     */
    public boolean enviarEmailConTemplate(String destinatario, String templateId, Map<String, Object> templateData) {
        log.info("[Resend] Enviando email (template ref: {}) a: {}", templateId, destinatario);

        StringBuilder html = new StringBuilder(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">"
        );
        templateData.forEach((k, v) ->
                html.append("<p><strong>").append(k).append(":</strong> ").append(v).append("</p>")
        );
        html.append("</div>");

        return enviarEmail(destinatario, "Notificación", html.toString());
    }

    // ══════════════════════════════════════════════════════════════════
    // EMAILS DE NEGOCIO
    // ══════════════════════════════════════════════════════════════════

    /**
     * Envía confirmación de registro.
     */
    public boolean enviarConfirmacionRegistro(String destinatario, String nombreUsuario) {
        String asunto = "Bienvenido a Cita Click";
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h1 style=\"color: #7c3aed;\">¡Bienvenido %s!</h1>" +
                "<p>Tu cuenta ha sido creada exitosamente en <strong>Cita Click</strong>.</p>" +
                "<p>Ya puedes comenzar a gestionar tus citas y clientes.</p>" +
                "</div>",
                nombreUsuario
        );
        return enviarEmail(destinatario, asunto, contenido);
    }

    /**
     * Envía recordatorio de cita usando el template Clásico de Resend.
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
        return enviarRecordatorioCita(destinatario, nombreCliente, fechaCita, horaCita,
                                     nombreServicio, nombreNegocio, PlantillaEmailConfig.TipoDiseno.CLASICO);
    }

    /**
     * Envía recordatorio de cita usando el template del diseño elegido por el negocio.
     *
     * @param diseno Diseño de la plantilla del negocio (CLASICO, MODERNO, MINIMALISTA)
     */
    public boolean enviarRecordatorioCita(String destinatario, String nombreCliente, String fechaCita,
                                          String horaCita, String nombreServicio, String nombreNegocio,
                                          PlantillaEmailConfig.TipoDiseno diseno) {
        String asunto = String.format("Recordatorio de cita - %s", nombreNegocio);
        String templateId = resolverTemplateRecordatorio(diseno);

        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCliente",  nombreCliente);
        variables.put("nombreServicio", nombreServicio);
        variables.put("fechaCita",      fechaCita);
        variables.put("horaCita",       horaCita);
        variables.put("nombreNegocio",  nombreNegocio);

        return enviarConTemplateResend(destinatario, asunto, templateId, variables);
    }

    /**
     * Sobrecarga por compatibilidad con código legado.
     * @deprecated Usar {@link #enviarRecordatorioCita(String, String, String, String, String, String)}
     */
    @Deprecated
    public boolean enviarRecordatorioCita(String destinatario, String nombreCliente, String fechaHora,
                                          String nombreServicio, String nombreNegocio) {
        return enviarRecordatorioCita(destinatario, nombreCliente, fechaHora, "",
                                     nombreServicio, nombreNegocio);
    }

    /**
     * Envía confirmación de cita.
     */
    public boolean enviarConfirmacionCita(String destinatario, String nombreCliente,
                                          String fechaHora, String nombreServicio) {
        String asunto = "Confirmación de cita - Cita Click";
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #7c3aed;\">Cita Confirmada ✓</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Tu cita ha sido confirmada exitosamente.</p>" +
                "<table style=\"width: 100%%; border-collapse: collapse; margin: 20px 0;\">" +
                "<tr style=\"background-color: #f5f3ff;\">" +
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
     */
    public boolean enviarEmailInvitacionUsuario(String destinatario, String nombreUsuario,
                                                String nombreNegocio, String passwordTemporal) {
        String asunto = String.format("Invitación a unirte a %s en Cita Click", nombreNegocio);
        String contenido = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #7c3aed;\">¡Has sido invitado!</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Has sido invitado a unirte a <strong>%s</strong> en Cita Click.</p>" +
                "<h3 style=\"color: #374151;\">Tus credenciales de acceso:</h3>" +
                "<table style=\"width: 100%%; border-collapse: collapse; margin: 20px 0;\">" +
                "<tr style=\"background-color: #f5f3ff;\">" +
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
     * Envía email de verificación de cuenta usando el template de Resend.
     * Variables en template: {@code {{nombre}}}, {@code {{verificationUrl}}}
     *
     * @param destinatario    Email del destinatario
     * @param nombreUsuario   Nombre del usuario
     * @param verificationUrl URL de verificación
     * @return true si se envió correctamente
     */
    public boolean enviarEmailVerificacion(String destinatario, String nombreUsuario, String verificationUrl) {
        String asunto = "Verifica tu cuenta en Cita Click";

        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre",          nombreUsuario);
        variables.put("verificationUrl", verificationUrl);

        return enviarConTemplateResend(destinatario, asunto, templateVerificacion, variables);
    }
}
