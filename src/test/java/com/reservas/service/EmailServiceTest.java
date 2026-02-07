package com.reservas.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para EmailService
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private SendGrid mockSendGrid;

    @Mock
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        // Configurar valores por defecto
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "SG.test-api-key-12345");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test System");
        ReflectionTestUtils.setField(emailService, "verificationTemplateId", "d-test-verification-template");
        ReflectionTestUtils.setField(emailService, "reminderTemplateId", "d-test-reminder-template");
    }

    @Test
    void testEnviarEmail_Exitoso() throws IOException {
        // Given
        String destinatario = "user@example.com";
        String asunto = "Test Subject";
        String contenido = "<h1>Test Content</h1>";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarEmail(destinatario, asunto, contenido);

            // Then
            assertTrue(result);
            assertEquals(1, mockedSendGrid.constructed().size());
        }
    }

    @Test
    void testEnviarEmail_SendGridNoConfigurado() {
        // Given
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "");
        String destinatario = "user@example.com";
        String asunto = "Test Subject";
        String contenido = "<h1>Test Content</h1>";

        // When
        boolean result = emailService.enviarEmail(destinatario, asunto, contenido);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarEmail_SendGridNoConfigurado_ApiKeyNull() {
        // Given
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", null);
        String destinatario = "user@example.com";
        String asunto = "Test Subject";
        String contenido = "<h1>Test Content</h1>";

        // When
        boolean result = emailService.enviarEmail(destinatario, asunto, contenido);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarEmail_ErrorStatusCode() throws IOException {
        // Given
        String destinatario = "user@example.com";
        String asunto = "Test Subject";
        String contenido = "<h1>Test Content</h1>";

        when(mockResponse.getStatusCode()).thenReturn(400);
        when(mockResponse.getBody()).thenReturn("Bad Request");

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarEmail(destinatario, asunto, contenido);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testEnviarEmail_IOException() throws IOException {
        // Given
        String destinatario = "user@example.com";
        String asunto = "Test Subject";
        String contenido = "<h1>Test Content</h1>";

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenThrow(new IOException("Network error")))) {

            // When
            boolean result = emailService.enviarEmail(destinatario, asunto, contenido);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testEnviarEmailConTemplate_Exitoso() throws IOException {
        // Given
        String destinatario = "user@example.com";
        String templateId = "d-test-template";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("nombre", "Juan");
        templateData.put("verificationUrl", "https://example.com/verify/123");

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarEmailConTemplate(destinatario, templateId, templateData);

            // Then
            assertTrue(result);
            assertEquals(1, mockedSendGrid.constructed().size());
        }
    }

    @Test
    void testEnviarEmailConTemplate_SendGridNoConfigurado() {
        // Given
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "");
        String destinatario = "user@example.com";
        String templateId = "d-test-template";
        Map<String, Object> templateData = Map.of("nombre", "Juan");

        // When
        boolean result = emailService.enviarEmailConTemplate(destinatario, templateId, templateData);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarEmailConTemplate_ErrorStatusCode() throws IOException {
        // Given
        String destinatario = "user@example.com";
        String templateId = "d-test-template";
        Map<String, Object> templateData = Map.of("nombre", "Juan");

        when(mockResponse.getStatusCode()).thenReturn(500);
        when(mockResponse.getBody()).thenReturn("Internal Server Error");

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarEmailConTemplate(destinatario, templateId, templateData);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testEnviarConfirmacionRegistro_Exitoso() throws IOException {
        // Given
        String destinatario = "newuser@example.com";
        String nombreUsuario = "María García";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarConfirmacionRegistro(destinatario, nombreUsuario);

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarRecordatorioCita_ConTemplateConfigurado() throws IOException {
        // Given
        String destinatario = "client@example.com";
        String nombreCliente = "Pedro López";
        String fechaCita = "Lunes 20 de Enero, 2026";
        String horaCita = "10:00 AM";
        String nombreServicio = "Corte de Cabello";
        String nombreNegocio = "Barbería El Clásico";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarRecordatorioCita(
                    destinatario, nombreCliente, fechaCita, horaCita, nombreServicio, nombreNegocio
            );

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarRecordatorioCita_SinTemplateConfigurado() throws IOException {
        // Given
        ReflectionTestUtils.setField(emailService, "reminderTemplateId", "");
        String destinatario = "client@example.com";
        String nombreCliente = "Pedro López";
        String fechaCita = "Lunes 20 de Enero, 2026";
        String horaCita = "10:00 AM";
        String nombreServicio = "Corte de Cabello";
        String nombreNegocio = "Barbería El Clásico";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarRecordatorioCita(
                    destinatario, nombreCliente, fechaCita, horaCita, nombreServicio, nombreNegocio
            );

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarRecordatorioCita_MetodoDeprecado() throws IOException {
        // Given
        String destinatario = "client@example.com";
        String nombreCliente = "Pedro López";
        String fechaHora = "Lunes 20 de Enero, 2026 - 10:00 AM";
        String nombreServicio = "Corte de Cabello";
        String nombreNegocio = "Barbería El Clásico";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            @SuppressWarnings("deprecation")
            boolean result = emailService.enviarRecordatorioCita(
                    destinatario, nombreCliente, fechaHora, nombreServicio, nombreNegocio
            );

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarConfirmacionCita_Exitoso() throws IOException {
        // Given
        String destinatario = "client@example.com";
        String nombreCliente = "Ana Martínez";
        String fechaHora = "Viernes 24 de Enero, 2026 - 3:00 PM";
        String nombreServicio = "Masaje Relajante";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarConfirmacionCita(
                    destinatario, nombreCliente, fechaHora, nombreServicio
            );

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarEmailInvitacionUsuario_Exitoso() throws IOException {
        // Given
        String destinatario = "newemployee@example.com";
        String nombreUsuario = "Carlos Ruiz";
        String nombreNegocio = "Spa Bienestar";
        String passwordTemporal = "Temp123!";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarEmailInvitacionUsuario(
                    destinatario, nombreUsuario, nombreNegocio, passwordTemporal
            );

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarEmailVerificacion_Exitoso() throws IOException {
        // Given
        String destinatario = "user@example.com";
        String nombreUsuario = "Laura Sánchez";
        String verificationUrl = "https://example.com/verify/abc123xyz";

        when(mockResponse.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mockedSendGrid = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class))).thenReturn(mockResponse))) {

            // When
            boolean result = emailService.enviarEmailVerificacion(
                    destinatario, nombreUsuario, verificationUrl
            );

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testEnviarEmailVerificacion_SendGridNoConfigurado() {
        // Given
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", null);
        String destinatario = "user@example.com";
        String nombreUsuario = "Laura Sánchez";
        String verificationUrl = "https://example.com/verify/abc123xyz";

        // When
        boolean result = emailService.enviarEmailVerificacion(
                destinatario, nombreUsuario, verificationUrl
        );

        // Then
        assertFalse(result);
    }
}
