package com.reservas.service;

import com.reservas.dto.request.LoginRequest;
import com.reservas.dto.request.RegisterRequest;
import com.reservas.dto.response.LoginResponse;
import com.reservas.dto.response.UserResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.UsuarioRepository;
import com.reservas.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Pruebas Unitarias")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private com.reservas.service.EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioMock;
    private Negocio negocioMock;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nombre("Negocio Test")
                .tipo("salon")
                .estadoPago("trial")
                .plan("starter")
                .fechaInicioPlan(LocalDateTime.now())
                .build();

        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("García")
                .telefono("1234567890")
                .negocio(negocioMock)
                .rol("admin")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Registrar - Debe crear usuario y negocio exitosamente")
    void testRegistrar_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("nuevo@example.com");
        request.setPassword("password123");
        request.setNombre("María");
        request.setApellidoPaterno("López");
        request.setApellidoMaterno("Sánchez");

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(negocioRepository.save(any(Negocio.class))).thenReturn(negocioMock);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioMock);
        when(jwtProvider.generateToken(anyString())).thenReturn("jwt-token-123");

        // Act
        LoginResponse response = authService.registrar(request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Juan", response.getNombre());
        assertEquals("test@example.com", response.getEmail());
        verify(usuarioRepository).existsByEmail("nuevo@example.com");
        verify(negocioRepository).save(any(Negocio.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Registrar - Debe lanzar excepción si email ya existe")
    void testRegistrar_EmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existente@example.com");
        request.setPassword("password123");
        request.setNombre("Test");

        when(usuarioRepository.existsByEmail("existente@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.registrar(request, httpServletRequest));
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("Login - Debe autenticar usuario correctamente")
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", usuarioMock.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateToken("test@example.com")).thenReturn("jwt-token-123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Juan", response.getNombre());
        assertEquals("test@example.com", response.getEmail());
        verify(usuarioRepository).findByEmail("test@example.com");
        verify(jwtProvider).generateToken("test@example.com");
    }

    @Test
    @DisplayName("Login - Debe lanzar excepción si usuario no existe")
    void testLogin_UserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@example.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.login(request));
        verify(jwtProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Login - Debe lanzar excepción si contraseña es incorrecta")
    void testLogin_WrongPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("wrongpassword", usuarioMock.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.login(request));
        verify(jwtProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Login - Debe lanzar excepción si usuario está inactivo")
    void testLogin_InactiveUser() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        usuarioMock.setActivo(false);

        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", usuarioMock.getPasswordHash())).thenReturn(true);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.login(request));
        verify(jwtProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("ObtenerUsuarioActual - Debe retornar usuario autenticado")
    void testObtenerUsuarioActual_Success() {
        // Arrange
        when(authentication.getName()).thenReturn("test@example.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuarioMock));

        // Act
        UserResponse response = authService.obtenerUsuarioActual();

        // Assert
        assertNotNull(response);
        assertEquals("Juan", response.getNombre());
        assertEquals("Pérez", response.getApellidoPaterno());
        assertEquals("García", response.getApellidoMaterno());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("admin", response.getRol());
        assertTrue(response.isActivo());
        assertEquals(negocioMock.getId(), response.getNegocioId());
        assertEquals("Negocio Test", response.getNombreNegocio());
        verify(usuarioRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("ObtenerUsuarioActual - Debe lanzar excepción si no está autenticado")
    void testObtenerUsuarioActual_NotAuthenticated() {
        // Arrange
        when(authentication.getName()).thenReturn("anonymousUser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.obtenerUsuarioActual());
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("ObtenerUsuarioActual - Debe lanzar excepción si usuario está inactivo")
    void testObtenerUsuarioActual_InactiveUser() {
        // Arrange
        usuarioMock.setActivo(false);
        when(authentication.getName()).thenReturn("test@example.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.obtenerUsuarioActual());
    }

    @Test
    @DisplayName("ObtenerPorEmail - Debe retornar usuario existente")
    void testObtenerPorEmail_Success() {
        // Arrange
        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuarioMock));

        // Act
        Usuario result = authService.obtenerPorEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Juan", result.getNombre());
        verify(usuarioRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("ObtenerPorEmail - Debe lanzar excepción si usuario no existe")
    void testObtenerPorEmail_NotFound() {
        // Arrange
        when(usuarioRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.obtenerPorEmail("noexiste@example.com"));
    }
}
