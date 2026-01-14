package com.reservas.service;

import com.reservas.dto.request.ServicioRequest;
import com.reservas.dto.response.ServicioResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.ServicioRepository;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioService - Pruebas Unitarias")
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @InjectMocks
    private ServicioService servicioService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private Servicio servicioMock;
    private ServicioRequest servicioRequestMock;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .email("negocio@test.com")
                .nombre("Salon de Belleza Test")
                .tipo("salon")
                .estadoPago("activo")
                .plan("professional")
                .fechaInicioPlan(LocalDateTime.now())
                .build();

        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@test.com")
                .passwordHash("$2a$10$hashedPassword")
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("García")
                .rol("admin")
                .activo(true)
                .negocio(negocioMock)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        servicioMock = Servicio.builder()
                .id("servicio-123")
                .nombre("Corte de Cabello")
                .descripcion("Corte de cabello profesional")
                .precio(new BigDecimal("150.00"))
                .duracionMinutos(30)
                .activo(true)
                .negocio(negocioMock)
                .build();

        servicioRequestMock = ServicioRequest.builder()
                .nombre("Corte de Cabello")
                .descripcion("Corte de cabello profesional")
                .precio(new BigDecimal("150.00"))
                .duracionMinutos(30)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Crear servicio - Exitoso")
    void testCrearServicio_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.save(any(Servicio.class))).thenReturn(servicioMock);

        // Act
        ServicioResponse response = servicioService.crearServicio("usuario@test.com", servicioRequestMock);

        // Assert
        assertNotNull(response);
        assertEquals("servicio-123", response.getId());
        assertEquals("Corte de Cabello", response.getNombre());
        assertEquals(new BigDecimal("150.00"), response.getPrecio());
        assertEquals(30, response.getDuracionMinutos());
        assertTrue(response.getActivo());

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(servicioRepository, times(1)).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Crear servicio - Usuario no encontrado")
    void testCrearServicio_UsuarioNoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            servicioService.crearServicio("usuario@inexistente.com", servicioRequestMock);
        });

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Crear servicio - Negocio no encontrado")
    void testCrearServicio_NegocioNoEncontrado() {
        // Arrange
        Usuario usuarioSinNegocio = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@test.com")
                .negocio(null)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioSinNegocio));

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            servicioService.crearServicio("usuario@test.com", servicioRequestMock);
        });
    }

    @Test
    @DisplayName("Listar servicios - Solo activos")
    void testListarServicios_SoloActivos() {
        // Arrange
        Servicio servicioActivo1 = Servicio.builder()
                .id("serv-1")
                .nombre("Servicio 1")
                .activo(true)
                .negocio(negocioMock)
                .precio(new BigDecimal("100.00"))
                .duracionMinutos(30)
                .build();

        Servicio servicioActivo2 = Servicio.builder()
                .id("serv-2")
                .nombre("Servicio 2")
                .activo(true)
                .negocio(negocioMock)
                .precio(new BigDecimal("200.00"))
                .duracionMinutos(60)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findByNegocioAndActivoTrue(any(Negocio.class)))
                .thenReturn(Arrays.asList(servicioActivo1, servicioActivo2));

        // Act
        List<ServicioResponse> response = servicioService.listarServicios("usuario@test.com", true);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("serv-1", response.get(0).getId());
        assertEquals("serv-2", response.get(1).getId());

        verify(servicioRepository, times(1)).findByNegocioAndActivoTrue(any(Negocio.class));
        verify(servicioRepository, never()).findByNegocio(any(Negocio.class));
    }

    @Test
    @DisplayName("Listar servicios - Todos")
    void testListarServicios_Todos() {
        // Arrange
        Servicio servicioActivo = Servicio.builder()
                .id("serv-1")
                .nombre("Servicio Activo")
                .activo(true)
                .negocio(negocioMock)
                .precio(new BigDecimal("100.00"))
                .duracionMinutos(30)
                .build();

        Servicio servicioInactivo = Servicio.builder()
                .id("serv-2")
                .nombre("Servicio Inactivo")
                .activo(false)
                .negocio(negocioMock)
                .precio(new BigDecimal("200.00"))
                .duracionMinutos(60)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findByNegocio(any(Negocio.class)))
                .thenReturn(Arrays.asList(servicioActivo, servicioInactivo));

        // Act
        List<ServicioResponse> response = servicioService.listarServicios("usuario@test.com", false);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());

        verify(servicioRepository, times(1)).findByNegocio(any(Negocio.class));
        verify(servicioRepository, never()).findByNegocioAndActivoTrue(any(Negocio.class));
    }

    @Test
    @DisplayName("Obtener servicio - Exitoso")
    void testObtenerServicio_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(anyString())).thenReturn(Optional.of(servicioMock));

        // Act
        ServicioResponse response = servicioService.obtenerServicio("usuario@test.com", "servicio-123");

        // Assert
        assertNotNull(response);
        assertEquals("servicio-123", response.getId());
        assertEquals("Corte de Cabello", response.getNombre());

        verify(servicioRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Obtener servicio - No autorizado")
    void testObtenerServicio_NoAutorizado() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();

        Servicio servicioOtroNegocio = Servicio.builder()
                .id("servicio-123")
                .nombre("Servicio de otro negocio")
                .negocio(otroNegocio)
                .precio(new BigDecimal("100.00"))
                .duracionMinutos(30)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(anyString())).thenReturn(Optional.of(servicioOtroNegocio));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            servicioService.obtenerServicio("usuario@test.com", "servicio-123");
        });
    }

    @Test
    @DisplayName("Actualizar servicio - Exitoso")
    void testActualizarServicio_Exitoso() {
        // Arrange
        ServicioRequest updateRequest = ServicioRequest.builder()
                .nombre("Corte Premium")
                .descripcion("Corte de cabello premium")
                .precio(new BigDecimal("200.00"))
                .duracionMinutos(45)
                .activo(true)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(anyString())).thenReturn(Optional.of(servicioMock));
        when(servicioRepository.save(any(Servicio.class))).thenReturn(servicioMock);

        // Act
        ServicioResponse response = servicioService.actualizarServicio("usuario@test.com", "servicio-123", updateRequest);

        // Assert
        assertNotNull(response);
        verify(servicioRepository, times(1)).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Eliminar servicio - Soft delete exitoso")
    void testEliminarServicio_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(anyString())).thenReturn(Optional.of(servicioMock));
        when(servicioRepository.save(any(Servicio.class))).thenReturn(servicioMock);

        // Act
        servicioService.eliminarServicio("usuario@test.com", "servicio-123");

        // Assert
        verify(servicioRepository, times(1)).save(any(Servicio.class));
        verify(servicioRepository, never()).delete(any(Servicio.class));
    }

    @Test
    @DisplayName("Eliminar servicio - Servicio no encontrado")
    void testEliminarServicio_ServicioNoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            servicioService.eliminarServicio("usuario@test.com", "servicio-inexistente");
        });

        verify(servicioRepository, never()).save(any(Servicio.class));
        verify(servicioRepository, never()).delete(any(Servicio.class));
    }
}
