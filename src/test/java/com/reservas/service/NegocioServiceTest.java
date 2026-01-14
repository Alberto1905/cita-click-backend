package com.reservas.service;

import com.reservas.dto.request.HorarioTrabajoRequest;
import com.reservas.dto.request.NegocioRequest;
import com.reservas.dto.response.NegocioResponse;
import com.reservas.entity.HorarioTrabajo;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.HorarioTrabajoRepository;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NegocioService - Pruebas Unitarias")
class NegocioServiceTest {

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HorarioTrabajoRepository horarioTrabajoRepository;

    @InjectMocks
    private NegocioService negocioService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private HorarioTrabajo horarioMock;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .email("negocio@example.com")
                .nombre("Mi Salón")
                .descripcion("Salón de belleza")
                .telefono("1234567890")
                .tipo("salon")
                .domicilio("Calle Principal 123")
                .ciudad("CDMX")
                .pais("México")
                .plan("professional")
                .estadoPago("active")
                .build();

        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@example.com")
                .nombre("Juan")
                .negocio(negocioMock)
                .rol("admin")
                .activo(true)
                .build();

        horarioMock = HorarioTrabajo.builder()
                .id(UUID.randomUUID())
                .negocio(negocioMock)
                .diaSemana(1)
                .horaApertura(LocalTime.of(9, 0))
                .horaCierre(LocalTime.of(18, 0))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("ObtenerNegocioDelUsuario - Debe retornar negocio del usuario")
    void testObtenerNegocioDelUsuario_Success() {
        // Arrange
        when(usuarioRepository.findByEmail("usuario@example.com")).thenReturn(Optional.of(usuarioMock));

        // Act
        NegocioResponse response = negocioService.obtenerNegocioDelUsuario("usuario@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("Mi Salón", response.getNombre());
        assertEquals("negocio@example.com", response.getEmail());
        assertEquals("salon", response.getTipo());
        assertEquals("professional", response.getPlan());
        verify(usuarioRepository).findByEmail("usuario@example.com");
    }

    @Test
    @DisplayName("ObtenerNegocioDelUsuario - Debe lanzar excepción si usuario no existe")
    void testObtenerNegocioDelUsuario_UserNotFound() {
        // Arrange
        when(usuarioRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class,
            () -> negocioService.obtenerNegocioDelUsuario("noexiste@example.com"));
    }

    @Test
    @DisplayName("ActualizarNegocio - Debe actualizar datos correctamente")
    void testActualizarNegocio_Success() {
        // Arrange
        NegocioRequest request = new NegocioRequest();
        request.setNombre("Nuevo Nombre");
        request.setDescripcion("Nueva descripción");
        request.setTelefono("9876543210");
        request.setCiudad("Guadalajara");

        when(usuarioRepository.findByEmail("usuario@example.com")).thenReturn(Optional.of(usuarioMock));
        when(negocioRepository.save(any(Negocio.class))).thenReturn(negocioMock);

        // Act
        NegocioResponse response = negocioService.actualizarNegocio("usuario@example.com", request);

        // Assert
        assertNotNull(response);
        verify(usuarioRepository).findByEmail("usuario@example.com");
        verify(negocioRepository).save(negocioMock);
    }

    @Test
    @DisplayName("ObtenerHorarios - Debe retornar lista de horarios")
    void testObtenerHorarios_Success() {
        // Arrange
        List<HorarioTrabajo> horarios = Arrays.asList(horarioMock);
        when(usuarioRepository.findByEmail("usuario@example.com")).thenReturn(Optional.of(usuarioMock));
        when(horarioTrabajoRepository.findByNegocio(negocioMock)).thenReturn(horarios);

        // Act
        List<HorarioTrabajo> result = negocioService.obtenerHorarios("usuario@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getDiaSemana());
        verify(horarioTrabajoRepository).findByNegocio(negocioMock);
    }

    @Test
    @DisplayName("GuardarHorario - Debe crear horario correctamente")
    void testGuardarHorario_Success() {
        // Arrange
        HorarioTrabajoRequest request = new HorarioTrabajoRequest();
        request.setHoraApertura(LocalTime.of(8, 0));
        request.setHoraCierre(LocalTime.of(20, 0));
        request.setActivo(true);

        when(usuarioRepository.findByEmail("usuario@example.com")).thenReturn(Optional.of(usuarioMock));
        when(horarioTrabajoRepository.save(any(HorarioTrabajo.class))).thenReturn(horarioMock);

        // Act
        HorarioTrabajo result = negocioService.guardarHorario("usuario@example.com", 2, request);

        // Assert
        assertNotNull(result);
        verify(usuarioRepository).findByEmail("usuario@example.com");
        verify(horarioTrabajoRepository).deleteByNegocioAndDiaSemana(negocioMock, 2);
        verify(horarioTrabajoRepository).save(any(HorarioTrabajo.class));
    }

    @Test
    @DisplayName("EliminarHorario - Debe eliminar horario correctamente")
    void testEliminarHorario_Success() {
        // Arrange
        UUID horarioId = horarioMock.getId();
        when(horarioTrabajoRepository.findById(horarioId)).thenReturn(Optional.of(horarioMock));
        when(usuarioRepository.findByEmail("usuario@example.com")).thenReturn(Optional.of(usuarioMock));

        // Act
        negocioService.eliminarHorario(horarioId, "usuario@example.com");

        // Assert
        verify(horarioTrabajoRepository).findById(horarioId);
        verify(horarioTrabajoRepository).deleteById(horarioId);
    }

    @Test
    @DisplayName("EliminarHorario - Debe lanzar excepción si horario no existe")
    void testEliminarHorario_HorarioNotFound() {
        // Arrange
        UUID horarioId = UUID.randomUUID();
        when(horarioTrabajoRepository.findById(horarioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> negocioService.eliminarHorario(horarioId, "usuario@example.com"));
        verify(horarioTrabajoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("EliminarHorario - Debe lanzar excepción si usuario no tiene permiso")
    void testEliminarHorario_Unauthorized() {
        // Arrange
        UUID horarioId = horarioMock.getId();
        Usuario otroUsuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("otro@example.com")
                .negocio(Negocio.builder().id(UUID.randomUUID()).build())
                .build();

        when(horarioTrabajoRepository.findById(horarioId)).thenReturn(Optional.of(horarioMock));
        when(usuarioRepository.findByEmail("otro@example.com")).thenReturn(Optional.of(otroUsuario));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
            () -> negocioService.eliminarHorario(horarioId, "otro@example.com"));
        verify(horarioTrabajoRepository, never()).deleteById(any());
    }
}
