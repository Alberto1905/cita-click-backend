package com.reservas.service;

import com.reservas.entity.PlantillaEmailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para EmailService (Resend API via RestTemplate).
 *
 * Usa el patrón de subclase de prueba (test-double) que sobrescribe el método
 * protegido doPost() para evitar llamadas HTTP reales y problemas con
 * la instrumentación de bytecode de Mockito en Java 23.
 */
class EmailServiceTest {

    // ─────────────────────────────────────────────────────────────────
    // Test double: subclase que captura las llamadas a doPost()
    // ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static class TestEmailService extends EmailService {

        private ResponseEntity<Map> responseToReturn;
        private RuntimeException exceptionToThrow;
        private int postCallCount = 0;
        /** Último cuerpo del request capturado en doPost */
        private HttpEntity<?> lastRequest;

        @Override
        protected ResponseEntity<Map> doPost(String url, HttpEntity<?> entity) {
            postCallCount++;
            lastRequest = entity;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return responseToReturn;
        }

        void willReturn(String id) {
            Map<String, Object> body = new HashMap<>();
            body.put("id", id);
            this.responseToReturn = ResponseEntity.ok(body);
            this.exceptionToThrow = null;
        }

        void willThrow(RuntimeException e) {
            this.exceptionToThrow = e;
            this.responseToReturn = null;
        }

        boolean wasPostCalled() {
            return postCallCount > 0;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> lastBody() {
            return lastRequest != null ? (Map<String, Object>) lastRequest.getBody() : null;
        }

        void resetCallCount() {
            postCallCount = 0;
            lastRequest = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────

    /** Template IDs que usa el servicio (igual que application.yml defaults) */
    private static final String TEMPLATE_VERIFICACION       = "verificacin-correo-electrnico";
    private static final String TEMPLATE_RECORDATORIO_CLA   = "recordatorio-clasico";
    private static final String TEMPLATE_RECORDATORIO_MOD   = "recordatorio-moderno";
    private static final String TEMPLATE_RECORDATORIO_MINI  = "recordatorio-minimalista";

    private TestEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new TestEmailService();
        ReflectionTestUtils.setField(emailService, "resendApiKey",               "re_test-api-key-12345");
        ReflectionTestUtils.setField(emailService, "fromEmail",                  "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "fromName",                   "Test System");
        // Template IDs
        ReflectionTestUtils.setField(emailService, "templateVerificacion",       TEMPLATE_VERIFICACION);
        ReflectionTestUtils.setField(emailService, "templateRecordatorioClasico",    TEMPLATE_RECORDATORIO_CLA);
        ReflectionTestUtils.setField(emailService, "templateRecordatorioModerno",    TEMPLATE_RECORDATORIO_MOD);
        ReflectionTestUtils.setField(emailService, "templateRecordatorioMinimalista",TEMPLATE_RECORDATORIO_MINI);
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmail (HTML inline)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmail_Exitoso() {
        emailService.willReturn("email-id-001");

        boolean result = emailService.enviarEmail("user@example.com", "Test Subject", "<h1>Content</h1>");

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
        // Debe enviar el campo "html", no template_id
        assertTrue(emailService.lastBody().containsKey("html"));
        assertFalse(emailService.lastBody().containsKey("template_id"));
    }

    @Test
    void testEnviarEmail_ResendNoConfigurado_ApiKeyVacia() {
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmail_ResendNoConfigurado_ApiKeyNull() {
        ReflectionTestUtils.setField(emailService, "resendApiKey", null);

        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmail_ErrorHTTP() {
        emailService.willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        assertFalse(result);
    }

    @Test
    void testEnviarEmail_ErrorInesperado() {
        emailService.willThrow(new RuntimeException("Connection refused"));

        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        assertFalse(result);
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmailConTemplate
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmailConTemplate_Exitoso() {
        emailService.willReturn("email-id-002");
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("nombre", "Juan");
        templateData.put("verificationUrl", "https://example.com/verify/123");

        boolean result = emailService.enviarEmailConTemplate("user@example.com", "ref-template-001", templateData);

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmailConTemplate_ResendNoConfigurado() {
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        boolean result = emailService.enviarEmailConTemplate(
                "user@example.com", "ref-template-001", Map.of("nombre", "Juan")
        );

        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarConfirmacionRegistro
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarConfirmacionRegistro_Exitoso() {
        emailService.willReturn("email-id-003");

        boolean result = emailService.enviarConfirmacionRegistro("newuser@example.com", "María García");

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarRecordatorioCita — templates de Resend
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarRecordatorioCita_Exitoso_TemplateClasico() {
        emailService.willReturn("email-id-004");

        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026", "10:00 AM",
                "Corte de Cabello", "Barbería El Clásico"
        );

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
        // Verifica que se envió con template_id y variables (no html inline)
        Map<String, Object> body = emailService.lastBody();
        assertEquals(TEMPLATE_RECORDATORIO_CLA, body.get("template_id"));
        assertTrue(body.containsKey("variables"));
        assertFalse(body.containsKey("html"));
    }

    @Test
    void testEnviarRecordatorioCita_TemplateModerno() {
        emailService.willReturn("email-id-004b");

        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026", "10:00 AM",
                "Corte de Cabello", "Barbería El Clásico",
                PlantillaEmailConfig.TipoDiseno.MODERNO
        );

        assertTrue(result);
        assertEquals(TEMPLATE_RECORDATORIO_MOD, emailService.lastBody().get("template_id"));
    }

    @Test
    void testEnviarRecordatorioCita_TemplateMinimalista() {
        emailService.willReturn("email-id-004c");

        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026", "10:00 AM",
                "Corte de Cabello", "Barbería El Clásico",
                PlantillaEmailConfig.TipoDiseno.MINIMALISTA
        );

        assertTrue(result);
        assertEquals(TEMPLATE_RECORDATORIO_MINI, emailService.lastBody().get("template_id"));
    }

    @Test
    void testEnviarRecordatorioCita_TemplateNoConfigurado() {
        // Si el template ID está vacío, no debe enviar
        ReflectionTestUtils.setField(emailService, "templateRecordatorioClasico", "");

        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026", "10:00 AM",
                "Corte de Cabello", "Barbería El Clásico"
        );

        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    @Test
    void testEnviarRecordatorioCita_VariablesCorrectas() {
        emailService.willReturn("email-id-004d");

        emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026", "10:00 AM",
                "Corte de Cabello", "Barbería El Clásico"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) emailService.lastBody().get("variables");
        assertNotNull(variables);
        assertEquals("Pedro López",              variables.get("nombreCliente"));
        assertEquals("Corte de Cabello",         variables.get("nombreServicio"));
        assertEquals("Lunes 20 de Enero, 2026",  variables.get("fechaCita"));
        assertEquals("10:00 AM",                 variables.get("horaCita"));
        assertEquals("Barbería El Clásico",      variables.get("nombreNegocio"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEnviarRecordatorioCita_MetodoDeprecado() {
        emailService.willReturn("email-id-005");

        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026 - 10:00 AM",
                "Corte de Cabello", "Barbería El Clásico"
        );

        assertTrue(result);
        // El método deprecated delega al método principal (que usa el template Clásico)
        assertEquals(TEMPLATE_RECORDATORIO_CLA, emailService.lastBody().get("template_id"));
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarConfirmacionCita
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarConfirmacionCita_Exitoso() {
        emailService.willReturn("email-id-006");

        boolean result = emailService.enviarConfirmacionCita(
                "client@example.com", "Ana Martínez",
                "Viernes 24 de Enero, 2026 - 3:00 PM", "Masaje Relajante"
        );

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmailInvitacionUsuario
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmailInvitacionUsuario_Exitoso() {
        emailService.willReturn("email-id-007");

        boolean result = emailService.enviarEmailInvitacionUsuario(
                "newemployee@example.com", "Carlos Ruiz", "Spa Bienestar", "Temp123!"
        );

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmailVerificacion — template de Resend
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmailVerificacion_Exitoso() {
        emailService.willReturn("email-id-008");

        boolean result = emailService.enviarEmailVerificacion(
                "user@example.com", "Laura Sánchez",
                "https://example.com/verify/abc123xyz"
        );

        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
        // Verifica que se envió con template_id y variables
        Map<String, Object> body = emailService.lastBody();
        assertEquals(TEMPLATE_VERIFICACION, body.get("template_id"));
        assertTrue(body.containsKey("variables"));
        assertFalse(body.containsKey("html"));
    }

    @Test
    void testEnviarEmailVerificacion_VariablesCorrectas() {
        emailService.willReturn("email-id-008b");

        emailService.enviarEmailVerificacion(
                "user@example.com", "Laura Sánchez",
                "https://example.com/verify/abc123xyz"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) emailService.lastBody().get("variables");
        assertNotNull(variables);
        assertEquals("Laura Sánchez",                    variables.get("nombre"));
        assertEquals("https://example.com/verify/abc123xyz", variables.get("verificationUrl"));
    }

    @Test
    void testEnviarEmailVerificacion_TemplateNoConfigurado() {
        // Si el template ID está vacío, no debe enviar
        ReflectionTestUtils.setField(emailService, "templateVerificacion", "");

        boolean result = emailService.enviarEmailVerificacion(
                "user@example.com", "Laura Sánchez",
                "https://example.com/verify/abc123xyz"
        );

        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmailVerificacion_ResendNoConfigurado() {
        ReflectionTestUtils.setField(emailService, "resendApiKey", null);

        boolean result = emailService.enviarEmailVerificacion(
                "user@example.com", "Laura Sánchez",
                "https://example.com/verify/abc123xyz"
        );

        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }
}
