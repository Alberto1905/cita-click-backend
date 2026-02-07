package com.reservas.service;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SmsService
 */
@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        // Configurar valores por defecto (vacíos para simular estado sin configurar)
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "");
        ReflectionTestUtils.setField(smsService, "twilioPhoneNumber", "+15551234567");
        ReflectionTestUtils.setField(smsService, "twilioWhatsAppNumber", "");
    }

    @Test
    void testEnviarSms_TwilioNoConfigurado() {
        // Given
        String telefono = "+525512345678";
        String mensaje = "Test message";

        // When
        boolean result = smsService.enviarSms(telefono, mensaje);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarSms_TwilioNoConfigurado_AccountSidNull() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", null);
        String telefono = "+525512345678";
        String mensaje = "Test message";

        // When
        boolean result = smsService.enviarSms(telefono, mensaje);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarSms_Exitoso() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        String telefono = "+525512345678";
        String mensaje = "Recordatorio de cita";

        MessageCreator mockCreator = mock(MessageCreator.class);
        Message mockMessage = mock(Message.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenReturn(mockMessage);
            when(mockMessage.getSid()).thenReturn("SM123456789");

            // When
            boolean result = smsService.enviarSms(telefono, mensaje);

            // Then
            assertTrue(result);
            verify(mockCreator).create();
        }
    }

    @Test
    void testEnviarSms_TelefonoInvalido() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        String telefonoInvalido = "invalid-phone";
        String mensaje = "Test message";

        MessageCreator mockCreator = mock(MessageCreator.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenThrow(new ApiException("Invalid phone number"));

            // When
            boolean result = smsService.enviarSms(telefonoInvalido, mensaje);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testEnviarSms_ErrorDeRed() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        String telefono = "+525512345678";
        String mensaje = "Test message";

        MessageCreator mockCreator = mock(MessageCreator.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenThrow(new RuntimeException("Network error"));

            // When
            boolean result = smsService.enviarSms(telefono, mensaje);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testEnviarWhatsApp_TwilioNoConfigurado() {
        // Given
        String telefono = "+525512345678";
        String mensaje = "Test WhatsApp message";

        // When
        boolean result = smsService.enviarWhatsApp(telefono, mensaje);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarWhatsApp_Exitoso_ConNumeroWhatsAppConfigurado() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        ReflectionTestUtils.setField(smsService, "twilioWhatsAppNumber", "whatsapp:+15559876543");
        String telefono = "+525512345678";
        String mensaje = "Hola desde WhatsApp";

        MessageCreator mockCreator = mock(MessageCreator.class);
        Message mockMessage = mock(Message.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenReturn(mockMessage);
            when(mockMessage.getSid()).thenReturn("SM987654321");

            // When
            boolean result = smsService.enviarWhatsApp(telefono, mensaje);

            // Then
            assertTrue(result);
            verify(mockCreator).create();
        }
    }

    @Test
    void testEnviarWhatsApp_Exitoso_SinNumeroWhatsAppConfigurado() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        ReflectionTestUtils.setField(smsService, "twilioWhatsAppNumber", "");
        String telefono = "+525512345678";
        String mensaje = "Hola desde WhatsApp";

        MessageCreator mockCreator = mock(MessageCreator.class);
        Message mockMessage = mock(Message.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenReturn(mockMessage);
            when(mockMessage.getSid()).thenReturn("SM987654321");

            // When
            boolean result = smsService.enviarWhatsApp(telefono, mensaje);

            // Then
            assertTrue(result);
            verify(mockCreator).create();
        }
    }

    @Test
    void testEnviarWhatsApp_Error() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        String telefono = "+525512345678";
        String mensaje = "Test WhatsApp message";

        MessageCreator mockCreator = mock(MessageCreator.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenThrow(new ApiException("WhatsApp not enabled"));

            // When
            boolean result = smsService.enviarWhatsApp(telefono, mensaje);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testEnviarRecordatorioCita_TwilioNoConfigurado() {
        // Given
        String telefono = "+525512345678";
        String nombreCliente = "Juan Pérez";
        String fechaHora = "Lunes 20 de Enero, 2026 - 10:00 AM";
        String nombreServicio = "Corte de Cabello";

        // When
        boolean result = smsService.enviarRecordatorioCita(telefono, nombreCliente, fechaHora, nombreServicio);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnviarRecordatorioCita_Exitoso() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");
        String telefono = "+525512345678";
        String nombreCliente = "María García";
        String fechaHora = "Viernes 24 de Enero, 2026 - 3:00 PM";
        String nombreServicio = "Masaje Relajante";

        MessageCreator mockCreator = mock(MessageCreator.class);
        Message mockMessage = mock(Message.class);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockCreator);

            when(mockCreator.create()).thenReturn(mockMessage);
            when(mockMessage.getSid()).thenReturn("SM111222333");

            // When
            boolean result = smsService.enviarRecordatorioCita(telefono, nombreCliente, fechaHora, nombreServicio);

            // Then
            assertTrue(result);
            verify(mockCreator).create();
        }
    }

    @Test
    void testInit_TwilioConfigurado() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "ACtest123");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_test");

        // When
        // El método @PostConstruct no se ejecuta automáticamente en tests
        // Lo llamamos manualmente para validar la lógica
        smsService.init();

        // Then
        // Si no lanza excepción, la inicialización fue exitosa
        // En producción, Twilio.init() se ejecutaría
    }

    @Test
    void testInit_TwilioNoConfigurado() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "");

        // When
        smsService.init();

        // Then
        // No debe lanzar excepción, solo loguear advertencia
        // La prueba pasa si no hay excepción
    }

    @Test
    void testInit_TwilioNoConfigurado_AccountSidNull() {
        // Given
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", null);
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", null);

        // When
        smsService.init();

        // Then
        // No debe lanzar excepción, solo loguear advertencia
    }
}
