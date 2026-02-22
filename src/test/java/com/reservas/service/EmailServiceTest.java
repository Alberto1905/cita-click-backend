package com.reservas.service;

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

        @Override
        protected ResponseEntity<Map> doPost(String url, HttpEntity<?> entity) {
            postCallCount++;
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

        void resetCallCount() {
            postCallCount = 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────

    private TestEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new TestEmailService();
        ReflectionTestUtils.setField(emailService, "resendApiKey", "re_test-api-key-12345");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test System");
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmail
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmail_Exitoso() {
        // Given
        emailService.willReturn("email-id-001");

        // When
        boolean result = emailService.enviarEmail("user@example.com", "Test Subject", "<h1>Content</h1>");

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmail_ResendNoConfigurado_ApiKeyVacia() {
        // Given
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        // When
        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        // Then
        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmail_ResendNoConfigurado_ApiKeyNull() {
        // Given
        ReflectionTestUtils.setField(emailService, "resendApiKey", null);

        // When
        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        // Then
        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmail_ErrorHTTP() {
        // Given
        emailService.willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // When
        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarEmail_ErrorInesperado() {
        // Given
        emailService.willThrow(new RuntimeException("Connection refused"));

        // When
        boolean result = emailService.enviarEmail("user@example.com", "Test", "<p>Content</p>");

        // Then
        assertFalse(result);
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmailConTemplate
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmailConTemplate_Exitoso() {
        // Given
        emailService.willReturn("email-id-002");
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("nombre", "Juan");
        templateData.put("verificationUrl", "https://example.com/verify/123");

        // When
        boolean result = emailService.enviarEmailConTemplate("user@example.com", "ref-template-001", templateData);

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmailConTemplate_ResendNoConfigurado() {
        // Given
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        // When
        boolean result = emailService.enviarEmailConTemplate(
                "user@example.com", "ref-template-001", Map.of("nombre", "Juan")
        );

        // Then
        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarConfirmacionRegistro
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarConfirmacionRegistro_Exitoso() {
        // Given
        emailService.willReturn("email-id-003");

        // When
        boolean result = emailService.enviarConfirmacionRegistro("newuser@example.com", "María García");

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarRecordatorioCita
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarRecordatorioCita_Exitoso() {
        // Given
        emailService.willReturn("email-id-004");

        // When
        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026", "10:00 AM",
                "Corte de Cabello", "Barbería El Clásico"
        );

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    @Test
    void testEnviarRecordatorioCita_MetodoDeprecado() {
        // Given
        emailService.willReturn("email-id-005");

        // When
        @SuppressWarnings("deprecation")
        boolean result = emailService.enviarRecordatorioCita(
                "client@example.com", "Pedro López",
                "Lunes 20 de Enero, 2026 - 10:00 AM",
                "Corte de Cabello", "Barbería El Clásico"
        );

        // Then
        assertTrue(result);
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarConfirmacionCita
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarConfirmacionCita_Exitoso() {
        // Given
        emailService.willReturn("email-id-006");

        // When
        boolean result = emailService.enviarConfirmacionCita(
                "client@example.com", "Ana Martínez",
                "Viernes 24 de Enero, 2026 - 3:00 PM", "Masaje Relajante"
        );

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmailInvitacionUsuario
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmailInvitacionUsuario_Exitoso() {
        // Given
        emailService.willReturn("email-id-007");

        // When
        boolean result = emailService.enviarEmailInvitacionUsuario(
                "newemployee@example.com", "Carlos Ruiz", "Spa Bienestar", "Temp123!"
        );

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    // ─────────────────────────────────────────────────────────────────
    // enviarEmailVerificacion
    // ─────────────────────────────────────────────────────────────────

    @Test
    void testEnviarEmailVerificacion_Exitoso() {
        // Given
        emailService.willReturn("email-id-008");

        // When
        boolean result = emailService.enviarEmailVerificacion(
                "user@example.com", "Laura Sánchez",
                "https://example.com/verify/abc123xyz"
        );

        // Then
        assertTrue(result);
        assertTrue(emailService.wasPostCalled());
    }

    @Test
    void testEnviarEmailVerificacion_ResendNoConfigurado() {
        // Given
        ReflectionTestUtils.setField(emailService, "resendApiKey", null);

        // When
        boolean result = emailService.enviarEmailVerificacion(
                "user@example.com", "Laura Sánchez",
                "https://example.com/verify/abc123xyz"
        );

        // Then
        assertFalse(result);
        assertFalse(emailService.wasPostCalled());
    }
}
