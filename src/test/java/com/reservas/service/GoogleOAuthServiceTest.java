package com.reservas.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.reservas.dto.response.GoogleUserInfo;
import com.reservas.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleOAuthService - Pruebas Unitarias")
class GoogleOAuthServiceTest {

    @Mock
    private GoogleIdTokenVerifier verifier;

    @Mock
    private GoogleIdToken googleIdToken;

    @Mock
    private Payload payload;

    private GoogleOAuthService googleOAuthService;
    private static final String TEST_CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    private static final String VALID_TOKEN = "valid-google-token";
    private static final String INVALID_TOKEN = "invalid-google-token";

    @BeforeEach
    void setUp() {
        // Crear una instancia del servicio sin inicializar el verifier real
        googleOAuthService = new GoogleOAuthService(TEST_CLIENT_ID);

        // Usar reflection para inyectar el verifier mock
        try {
            java.lang.reflect.Field verifierField = GoogleOAuthService.class.getDeclaredField("verifier");
            verifierField.setAccessible(true);
            verifierField.set(googleOAuthService, verifier);
        } catch (Exception e) {
            throw new RuntimeException("Error al inyectar mock", e);
        }
    }

    @Test
    @DisplayName("Debe verificar token válido y retornar información completa del usuario")
    void debeVerificarTokenValido_yRetornarInformacionCompleta() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn("google-user-123");
        when(payload.getEmail()).thenReturn("test@gmail.com");
        when(payload.get("given_name")).thenReturn("Juan");
        when(payload.get("family_name")).thenReturn("Pérez");
        when(payload.get("name")).thenReturn("Juan Pérez");
        when(payload.get("picture")).thenReturn("https://lh3.googleusercontent.com/a/photo.jpg");

        // Act
        GoogleUserInfo result = googleOAuthService.verifyGoogleToken(VALID_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals("google-user-123", result.getGoogleId());
        assertEquals("test@gmail.com", result.getEmail());
        assertEquals("Juan", result.getNombre());
        assertEquals("Pérez", result.getApellido());
        assertEquals("Juan Pérez", result.getNombreCompleto());
        assertEquals("https://lh3.googleusercontent.com/a/photo.jpg", result.getImageUrl());
        assertTrue(result.isEmailVerified());

        verify(verifier).verify(VALID_TOKEN);
        verify(googleIdToken).getPayload();
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedException cuando el token es inválido")
    void debeLanzarUnauthorizedException_cuandoTokenEsInvalido() throws Exception {
        // Arrange
        when(verifier.verify(INVALID_TOKEN)).thenReturn(null);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> googleOAuthService.verifyGoogleToken(INVALID_TOKEN));

        assertTrue(exception.getMessage().contains("Token de Google inválido o expirado"));
        verify(verifier).verify(INVALID_TOKEN);
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedException cuando el email no está verificado")
    void debeLanzarUnauthorizedException_cuandoEmailNoEstaVerificado() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(false);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> googleOAuthService.verifyGoogleToken(VALID_TOKEN));

        assertTrue(exception.getMessage().contains("Email de Google no verificado"));
        verify(verifier).verify(VALID_TOKEN);
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedException cuando emailVerified es null")
    void debeLanzarUnauthorizedException_cuandoEmailVerifiedEsNull() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(null);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> googleOAuthService.verifyGoogleToken(VALID_TOKEN));

        assertTrue(exception.getMessage().contains("Email de Google no verificado"));
    }

    @Test
    @DisplayName("Debe manejar payload con campos opcionales nulos usando valores por defecto")
    void debeManejarPayloadConCamposOpcionalesNulos() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn("google-user-456");
        when(payload.getEmail()).thenReturn("user@gmail.com");
        when(payload.get("given_name")).thenReturn(null);
        when(payload.get("family_name")).thenReturn(null);
        when(payload.get("name")).thenReturn(null);
        when(payload.get("picture")).thenReturn(null);

        // Act
        GoogleUserInfo result = googleOAuthService.verifyGoogleToken(VALID_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals("google-user-456", result.getGoogleId());
        assertEquals("user@gmail.com", result.getEmail());
        assertEquals("", result.getNombre());
        assertEquals("", result.getApellido());
        assertEquals("user@gmail.com", result.getNombreCompleto());
        assertNull(result.getImageUrl());
        assertTrue(result.isEmailVerified());
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedException cuando el verifier lanza excepción")
    void debeLanzarUnauthorizedException_cuandoVerifierLanzaExcepcion() throws Exception {
        // Arrange
        when(verifier.verify(anyString()))
            .thenThrow(new RuntimeException("Error de red al validar token"));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> googleOAuthService.verifyGoogleToken(VALID_TOKEN));

        assertTrue(exception.getMessage().contains("Error al verificar token de Google"));
        assertTrue(exception.getMessage().contains("Error de red al validar token"));
    }

    @Test
    @DisplayName("Debe extraer correctamente given_name del payload")
    void debeExtraerCorrectamenteGivenName() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn("google-user-789");
        when(payload.getEmail()).thenReturn("maria@gmail.com");
        when(payload.get("given_name")).thenReturn("María");
        when(payload.get("family_name")).thenReturn("García");
        when(payload.get("name")).thenReturn("María García");
        when(payload.get("picture")).thenReturn("https://image.url");

        // Act
        GoogleUserInfo result = googleOAuthService.verifyGoogleToken(VALID_TOKEN);

        // Assert
        assertEquals("María", result.getNombre());
        assertEquals("García", result.getApellido());
        assertEquals("María García", result.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe usar email como nombreCompleto cuando name es null")
    void debeUsarEmailComoNombreCompleto_cuandoNameEsNull() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn("google-user-000");
        when(payload.getEmail()).thenReturn("test.user@gmail.com");
        when(payload.get("given_name")).thenReturn("Test");
        when(payload.get("family_name")).thenReturn("User");
        when(payload.get("name")).thenReturn(null);
        when(payload.get("picture")).thenReturn(null);

        // Act
        GoogleUserInfo result = googleOAuthService.verifyGoogleToken(VALID_TOKEN);

        // Assert
        assertEquals("test.user@gmail.com", result.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe manejar token expirado correctamente")
    void debeManejarTokenExpirado() throws Exception {
        // Arrange - Token expirado retorna null
        when(verifier.verify(INVALID_TOKEN)).thenReturn(null);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> googleOAuthService.verifyGoogleToken(INVALID_TOKEN));

        assertTrue(exception.getMessage().contains("Token de Google inválido o expirado"));
    }

    @Test
    @DisplayName("Debe verificar token con caracteres especiales en nombre")
    void debeVerificarTokenConCaracteresEspeciales() throws Exception {
        // Arrange
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn("google-user-special");
        when(payload.getEmail()).thenReturn("jose@gmail.com");
        when(payload.get("given_name")).thenReturn("José");
        when(payload.get("family_name")).thenReturn("Ramírez");
        when(payload.get("name")).thenReturn("José Ramírez");
        when(payload.get("picture")).thenReturn("https://photo.url");

        // Act
        GoogleUserInfo result = googleOAuthService.verifyGoogleToken(VALID_TOKEN);

        // Assert
        assertEquals("José", result.getNombre());
        assertEquals("Ramírez", result.getApellido());
        assertEquals("José Ramírez", result.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe manejar error de verificación con mensaje personalizado")
    void debeManejarErrorVerificacionConMensajePersonalizado() throws Exception {
        // Arrange
        when(verifier.verify(anyString()))
            .thenThrow(new IllegalArgumentException("Token mal formado"));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
            () -> googleOAuthService.verifyGoogleToken("malformed-token"));

        assertTrue(exception.getMessage().contains("Error al verificar token de Google"));
        assertTrue(exception.getMessage().contains("Token mal formado"));
    }

    @Test
    @DisplayName("Debe extraer googleId del subject del payload correctamente")
    void debeExtraerGoogleIdDelSubject() throws Exception {
        // Arrange
        String expectedGoogleId = "123456789012345678901";
        when(verifier.verify(VALID_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn(expectedGoogleId);
        when(payload.getEmail()).thenReturn("user@gmail.com");
        when(payload.get("given_name")).thenReturn("User");
        when(payload.get("family_name")).thenReturn("Test");
        when(payload.get("name")).thenReturn("User Test");
        when(payload.get("picture")).thenReturn("https://pic.url");

        // Act
        GoogleUserInfo result = googleOAuthService.verifyGoogleToken(VALID_TOKEN);

        // Assert
        assertEquals(expectedGoogleId, result.getGoogleId());
    }
}
