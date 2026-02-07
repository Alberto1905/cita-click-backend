package com.reservas.service;

import com.reservas.entity.Usuario;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService - Pruebas Unitarias")
class EmailVerificationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@test.com")
                .nombre("Juan Pérez")
                .emailVerificado(false)
                .build();

        // Inyectar el valor de frontendUrl
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:5174");
    }

    @Test
    @DisplayName("Debe enviar email de verificación correctamente")
    void debeEnviarEmailVerificacion_correctamente() {
        // Arrange
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioMock);
        when(emailService.enviarEmailVerificacion(anyString(), anyString(), anyString()))
                .thenReturn(true);

        // Act
        emailVerificationService.enviarEmailVerificacion(usuarioMock);

        // Assert
        assertNotNull(usuarioMock.getTokenVerificacion());
        assertNotNull(usuarioMock.getTokenVerificacionExpira());
        verify(usuarioRepository, times(1)).save(usuarioMock);
        verify(emailService, times(1)).enviarEmailVerificacion(
                eq("usuario@test.com"),
                eq("Juan Pérez"),
                anyString()
        );
    }

    @Test
    @DisplayName("Debe generar token único para cada usuario")
    void debeGenerarTokenUnico_paraCadaUsuario() {
        // Arrange
        Usuario usuario2 = Usuario.builder()
                .id(UUID.randomUUID())
                .email("otro@test.com")
                .nombre("María García")
                .emailVerificado(false)
                .build();

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        emailVerificationService.enviarEmailVerificacion(usuarioMock);
        emailVerificationService.enviarEmailVerificacion(usuario2);

        // Assert
        assertNotNull(usuarioMock.getTokenVerificacion());
        assertNotNull(usuario2.getTokenVerificacion());
        assertNotEquals(usuarioMock.getTokenVerificacion(), usuario2.getTokenVerificacion());
    }

    @Test
    @DisplayName("Debe verificar email exitosamente con token válido")
    void debeVerificarEmail_conTokenValido() {
        // Arrange
        String token = "token-valido-123";
        usuarioMock.setTokenVerificacion(token);
        usuarioMock.setTokenVerificacionExpira(LocalDateTime.now().plusHours(24));

        when(usuarioRepository.findByTokenVerificacion(token))
                .thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioMock);

        // Act
        boolean resultado = emailVerificationService.verificarEmail(token);

        // Assert
        assertTrue(resultado);
        assertTrue(usuarioMock.isEmailVerificado());
        assertNull(usuarioMock.getTokenVerificacion());
        assertNull(usuarioMock.getTokenVerificacionExpira());
        verify(usuarioRepository, times(1)).save(usuarioMock);
    }

    @Test
    @DisplayName("Debe rechazar verificación con token no encontrado")
    void debeRechazarVerificacion_conTokenNoEncontrado() {
        // Arrange
        String token = "token-inexistente";
        when(usuarioRepository.findByTokenVerificacion(token))
                .thenReturn(Optional.empty());

        // Act
        boolean resultado = emailVerificationService.verificarEmail(token);

        // Assert
        assertFalse(resultado);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe rechazar verificación con token expirado")
    void debeRechazarVerificacion_conTokenExpirado() {
        // Arrange
        String token = "token-expirado";
        usuarioMock.setTokenVerificacion(token);
        usuarioMock.setTokenVerificacionExpira(LocalDateTime.now().minusHours(1));

        when(usuarioRepository.findByTokenVerificacion(token))
                .thenReturn(Optional.of(usuarioMock));

        // Act
        boolean resultado = emailVerificationService.verificarEmail(token);

        // Assert
        assertFalse(resultado);
        assertFalse(usuarioMock.isEmailVerificado());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe reenviar email de verificación correctamente")
    void debeReenviarEmailVerificacion_correctamente() {
        // Arrange
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioMock);

        // Act
        emailVerificationService.reenviarEmailVerificacion("usuario@test.com");

        // Assert
        verify(usuarioRepository, times(1)).findByEmail("usuario@test.com");
        verify(usuarioRepository, times(1)).save(usuarioMock);
        verify(emailService, times(1)).enviarEmailVerificacion(
                eq("usuario@test.com"),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción al reenviar si usuario no existe")
    void debeLanzarExcepcion_alReenviar_siUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail("noexiste@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailVerificationService.reenviarEmailVerificacion("noexiste@test.com"));

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(emailService, never()).enviarEmailVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe lanzar excepción al reenviar si email ya está verificado")
    void debeLanzarExcepcion_alReenviar_siEmailYaVerificado() {
        // Arrange
        usuarioMock.setEmailVerificado(true);
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailVerificationService.reenviarEmailVerificacion("usuario@test.com"));

        assertEquals("El email ya está verificado", exception.getMessage());
        verify(emailService, never()).enviarEmailVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe verificar correctamente si email está verificado")
    void debeVerificarCorrectamente_siEmailEstaVerificado() {
        // Arrange
        usuarioMock.setEmailVerificado(true);
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));

        // Act
        boolean resultado = emailVerificationService.isEmailVerificado("usuario@test.com");

        // Assert
        assertTrue(resultado);
        verify(usuarioRepository, times(1)).findByEmail("usuario@test.com");
    }

    @Test
    @DisplayName("Debe retornar false si usuario no existe al verificar estado")
    void debeRetornarFalse_siUsuarioNoExiste_alVerificarEstado() {
        // Arrange
        when(usuarioRepository.findByEmail("noexiste@test.com"))
                .thenReturn(Optional.empty());

        // Act
        boolean resultado = emailVerificationService.isEmailVerificado("noexiste@test.com");

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Debe establecer fecha de expiración correctamente")
    void debeEstablecerFechaExpiracion_correctamente() {
        // Arrange
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now().plusHours(23);

        // Act
        emailVerificationService.enviarEmailVerificacion(usuarioMock);

        // Assert
        assertNotNull(usuarioMock.getTokenVerificacionExpira());
        assertTrue(usuarioMock.getTokenVerificacionExpira().isAfter(antes));
        assertTrue(usuarioMock.getTokenVerificacionExpira().isBefore(LocalDateTime.now().plusHours(25)));
    }
}
