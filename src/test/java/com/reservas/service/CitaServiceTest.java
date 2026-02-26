package com.reservas.service;

import com.reservas.dto.request.CitaRequest;
import com.reservas.dto.response.CitaResponse;
import com.reservas.entity.*;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.*;
import com.reservas.entity.enums.TipoPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitaService - Pruebas Unitarias")
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HorarioTrabajoRepository horarioTrabajoRepository;

    @Mock
    private DiaLibreRepository diaLibreRepository;

    // PlanLimitesService se añadió a CitaService para validar el límite mensual de citas
    @Mock
    private PlanLimitesService planLimitesService;

    @InjectMocks
    private CitaService citaService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private Cliente clienteMock;
    private Servicio servicioMock;
    private Cita citaMock;
    private CitaRequest citaRequestMock;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .email("negocio@test.com")
                .nombre("Salon de Belleza Test")
                .tipo("salon")
                .estadoPago("activo")
                .plan("profesional")
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
                .build();

        clienteMock = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("María")
                .apellidoPaterno("González")
                .apellidoMaterno("López")
                .email("maria@cliente.com")
                .telefono("1234567890")
                .negocio(negocioMock)
                .build();

        servicioMock = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Corte de Cabello")
                .descripcion("Corte profesional")
                .precio(new BigDecimal("150.00"))
                .duracionMinutos(30)
                .activo(true)
                .negocio(negocioMock)
                .build();

        LocalDateTime fechaHora = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDate fechaCita = LocalDate.of(2024, 1, 15);
        LocalTime horaCita = LocalTime.of(10, 0);

        citaMock = Cita.builder()
                .id("cita-123")
                .fechaHora(fechaHora)
                .estado(Cita.EstadoCita.PENDIENTE)
                .notas("Primera cita")
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        citaRequestMock = CitaRequest.builder()
                .fecha(fechaCita)
                .hora(horaCita)
                .clienteId(clienteMock.getId().toString())   // UUID válido
                .servicioId(servicioMock.getId().toString()) // UUID válido
                .notas("Primera cita")
                .build();
    }

    @Test
    @DisplayName("Crear cita - Exitoso")
    void testCrearCita_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioMock));
        when(citaRepository.save(any(Cita.class))).thenReturn(citaMock);

        // Act
        CitaResponse response = citaService.crearCita("usuario@test.com", citaRequestMock);

        // Assert
        assertNotNull(response);
        assertEquals("cita-123", response.getId());
        assertEquals("PENDIENTE", response.getEstado());
        assertEquals(clienteMock.getId(), response.getClienteId());
        assertEquals(servicioMock.getId(), response.getServicioId());

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(clienteRepository, times(1)).findById(any(UUID.class));
        verify(servicioRepository, times(1)).findById(any(UUID.class));
        verify(citaRepository, times(1)).save(any(Cita.class));
    }

    @Test
    @DisplayName("Crear cita - Cliente no encontrado")
    void testCrearCita_ClienteNoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            citaService.crearCita("usuario@test.com", citaRequestMock);
        });

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Crear cita - Servicio no encontrado")
    void testCrearCita_ServicioNoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            citaService.crearCita("usuario@test.com", citaRequestMock);
        });

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Crear cita - Servicio inactivo")
    void testCrearCita_ServicioInactivo() {
        // Arrange
        Servicio servicioInactivo = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Servicio Inactivo")
                .activo(false)
                .negocio(negocioMock)
                .duracionMinutos(30)
                .precio(new BigDecimal("100.00"))
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioInactivo));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            citaService.crearCita("usuario@test.com", citaRequestMock);
        });

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Crear cita - Cliente no pertenece al negocio")
    void testCrearCita_ClienteNoPertenece() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();

        Cliente clienteOtroNegocio = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("Cliente de otro negocio")
                .apellidoPaterno("Test")
                .negocio(otroNegocio)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteOtroNegocio));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            citaService.crearCita("usuario@test.com", citaRequestMock);
        });

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Crear cita - Horario no disponible")
    void testCrearCita_HorarioNoDisponible() {
        // Arrange
        LocalDateTime fechaHoraExistente = citaRequestMock.getFechaHora();
        Cita citaExistente = Cita.builder()
                .id("cita-existente")
                .fechaHora(fechaHoraExistente)
                .fechaFin(fechaHoraExistente.plusMinutes(servicioMock.getDuracionMinutos()))
                .estado(Cita.EstadoCita.CONFIRMADA)
                .negocio(negocioMock)
                .servicio(servicioMock)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioMock));
        when(citaRepository.findByNegocioAndFecha(any(), any()))
                .thenReturn(Arrays.asList(citaExistente));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            citaService.crearCita("usuario@test.com", citaRequestMock);
        });

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Listar citas - Por fecha")
    void testListarCitas_PorFecha() {
        // Arrange
        LocalDate fecha = LocalDate.of(2024, 1, 15);
        Cita cita1 = Cita.builder()
                .id("cita-1")
                .fechaHora(fecha.atTime(10, 0))
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(Arrays.asList(cita1));

        // Act
        List<CitaResponse> response = citaService.listarCitas("usuario@test.com", fecha, null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("cita-1", response.get(0).getId());

        verify(citaRepository, times(1)).findByNegocioAndFechaHoraBetween(any(), any(), any());
    }

    @Test
    @DisplayName("Listar citas - Por estado")
    void testListarCitas_PorEstado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndEstado(any(), any()))
                .thenReturn(Arrays.asList(citaMock));

        // Act
        List<CitaResponse> response = citaService.listarCitas("usuario@test.com", null, "PENDIENTE");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());

        verify(citaRepository, times(1)).findByNegocioAndEstado(any(), any());
    }

    @Test
    @DisplayName("Obtener cita - Exitoso")
    void testObtenerCita_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findById(anyString())).thenReturn(Optional.of(citaMock));

        // Act
        CitaResponse response = citaService.obtenerCita("usuario@test.com", "cita-123");

        // Assert
        assertNotNull(response);
        assertEquals("cita-123", response.getId());

        verify(citaRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Obtener cita - No autorizado")
    void testObtenerCita_NoAutorizado() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();

        Cita citaOtroNegocio = Cita.builder()
                .id("cita-123")
                .fechaHora(LocalDateTime.now())
                .negocio(otroNegocio)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findById(anyString())).thenReturn(Optional.of(citaOtroNegocio));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            citaService.obtenerCita("usuario@test.com", "cita-123");
        });
    }

    @Test
    @DisplayName("Actualizar cita - Exitoso")
    void testActualizarCita_Exitoso() {
        // Arrange
        CitaRequest updateRequest = CitaRequest.builder()
                .fecha(LocalDate.of(2024, 1, 15))
                .hora(LocalTime.of(14, 0))
                .clienteId(clienteMock.getId().toString())
                .servicioId(servicioMock.getId().toString())
                .notas("Cita actualizada")
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findById(anyString())).thenReturn(Optional.of(citaMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioMock));
        when(citaRepository.save(any(Cita.class))).thenReturn(citaMock);

        // Act
        CitaResponse response = citaService.actualizarCita("usuario@test.com", "cita-123", updateRequest);

        // Assert
        assertNotNull(response);
        verify(citaRepository, times(1)).save(any(Cita.class));
    }

    @Test
    @DisplayName("Cambiar estado cita - Exitoso")
    void testCambiarEstadoCita_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findById(anyString())).thenReturn(Optional.of(citaMock));
        when(citaRepository.save(any(Cita.class))).thenReturn(citaMock);

        // Act
        CitaResponse response = citaService.cambiarEstadoCita("usuario@test.com", "cita-123", "CONFIRMADA");

        // Assert
        assertNotNull(response);
        verify(citaRepository, times(1)).save(any(Cita.class));
    }

    @Test
    @DisplayName("Cambiar estado cita - Estado inválido")
    void testCambiarEstadoCita_EstadoInvalido() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findById(anyString())).thenReturn(Optional.of(citaMock));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            citaService.cambiarEstadoCita("usuario@test.com", "cita-123", "ESTADO_INVALIDO");
        });

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Cancelar cita - Exitoso")
    void testCancelarCita_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findById(anyString())).thenReturn(Optional.of(citaMock));
        when(citaRepository.save(any(Cita.class))).thenReturn(citaMock);

        // Act
        citaService.cancelarCita("usuario@test.com", "cita-123");

        // Assert
        verify(citaRepository, times(1)).save(any(Cita.class));
    }

    @Test
    @DisplayName("Obtener horarios disponibles - Sin días libres")
    void testObtenerHorariosDisponibles_SinDiasLibres() {
        // Arrange
        LocalDate fecha = LocalDate.of(2024, 1, 15); // Lunes

        HorarioTrabajo horario = HorarioTrabajo.builder()
                .id(UUID.randomUUID())
                .negocio(negocioMock)
                .diaSemana(0) // Lunes
                .horaApertura(LocalTime.of(9, 0))
                .horaCierre(LocalTime.of(18, 0))
                .activo(true)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(any(), any())).thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(), anyInt()))
                .thenReturn(Arrays.asList(horario));
        when(citaRepository.findByNegocioAndFecha(any(), any())).thenReturn(Collections.emptyList());

        // Act
        List<LocalDateTime> horarios = citaService.obtenerHorariosDisponibles("usuario@test.com", servicioMock.getId().toString(), fecha);

        // Assert
        assertNotNull(horarios);
        assertTrue(horarios.size() > 0);

        verify(diaLibreRepository, times(1)).findByNegocioAndFecha(any(), any());
        verify(horarioTrabajoRepository, times(1)).findByNegocioAndDiaSemana(any(), anyInt());
    }

    @Test
    @DisplayName("Obtener horarios disponibles - Es día libre")
    void testObtenerHorariosDisponibles_EsDiaLibre() {
        // Arrange
        LocalDate fecha = LocalDate.of(2024, 1, 1); // Día festivo

        DiaLibre diaLibre = DiaLibre.builder()
                .id("dia-libre-1")
                .negocio(negocioMock)
                .fecha(fecha)
                .razon("Año Nuevo")
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(any(), any())).thenReturn(Arrays.asList(diaLibre));

        // Act
        List<LocalDateTime> horarios = citaService.obtenerHorariosDisponibles("usuario@test.com", servicioMock.getId().toString(), fecha);

        // Assert
        assertNotNull(horarios);
        assertEquals(0, horarios.size());

        verify(diaLibreRepository, times(1)).findByNegocioAndFecha(any(), any());
        verify(horarioTrabajoRepository, never()).findByNegocioAndDiaSemana(any(), anyInt());
    }

    @Test
    @DisplayName("Obtener horarios disponibles - Sin horario configurado")
    void testObtenerHorariosDisponibles_SinHorarioConfigurado() {
        // Arrange
        LocalDate fecha = LocalDate.of(2024, 1, 15);

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(any(), any())).thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        List<LocalDateTime> horarios = citaService.obtenerHorariosDisponibles("usuario@test.com", servicioMock.getId().toString(), fecha);

        // Assert
        assertNotNull(horarios);
        assertEquals(0, horarios.size());

        verify(horarioTrabajoRepository, times(1)).findByNegocioAndDiaSemana(any(), anyInt());
    }
}
