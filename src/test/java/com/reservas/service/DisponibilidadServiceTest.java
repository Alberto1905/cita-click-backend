package com.reservas.service;

import com.reservas.dto.request.DisponibilidadRequest;
import com.reservas.dto.response.DisponibilidadResponse;
import com.reservas.entity.*;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.*;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisponibilidadService - Pruebas Unitarias")
class DisponibilidadServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private HorarioTrabajoRepository horarioTrabajoRepository;

    @Mock
    private DiaLibreRepository diaLibreRepository;

    @Mock
    private CitaRepository citaRepository;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private Servicio servicioMock;
    private HorarioTrabajo horarioTrabajoMock;
    private DisponibilidadRequest requestMock;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Salon Test")
                .email("salon@test.com")
                .tipo("salon")
                .plan("profesional")
                .estadoPago("activo")
                .build();

        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@test.com")
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .negocio(negocioMock)
                .build();

        servicioMock = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Corte de Cabello")
                .duracionMinutos(30)
                .precio(BigDecimal.valueOf(150.0))
                .activo(true)
                .negocio(negocioMock)
                .build();

        horarioTrabajoMock = HorarioTrabajo.builder()
                .id(UUID.randomUUID())
                .diaSemana(1) // Martes (0=Lunes, 1=Martes, etc.)
                .horaApertura(LocalTime.of(9, 0))
                .horaCierre(LocalTime.of(18, 0))
                .activo(true)
                .negocio(negocioMock)
                .build();

        requestMock = new DisponibilidadRequest();
        requestMock.setFecha(LocalDate.now().plusDays(7)); // Una semana adelante
        requestMock.setServicioIds(Collections.singletonList(servicioMock.getId().toString()));
    }

    @Test
    @DisplayName("Debe calcular horarios disponibles correctamente sin citas existentes")
    void debeCalcularHorariosDisponibles_sinCitasExistentes() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(Collections.emptyList());

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        assertEquals(requestMock.getFecha().toString(), response.getFecha());
        assertEquals(30, response.getDuracionTotal());
        assertFalse(response.getHorariosDisponibles().isEmpty());

        verify(usuarioRepository).findByEmail(usuarioMock.getEmail());
        verify(servicioRepository).findById(servicioMock.getId());
        verify(diaLibreRepository).findByNegocioAndFecha(negocioMock, requestMock.getFecha());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando el usuario no existe")
    void debeLanzarNotFoundException_cuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> disponibilidadService.obtenerHorariosDisponibles("noexiste@test.com", requestMock));

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usuarioRepository).findByEmail("noexiste@test.com");
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando el negocio no existe")
    void debeLanzarNotFoundException_cuandoNegocioNoExiste() {
        // Arrange
        usuarioMock.setNegocio(null);
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> disponibilidadService.obtenerHorariosDisponibles(usuarioMock.getEmail(), requestMock));

        assertEquals("Negocio no encontrado", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar BadRequestException para fechas pasadas")
    void debeLanzarBadRequestException_paraFechasPasadas() {
        // Arrange
        requestMock.setFecha(LocalDate.now().minusDays(1));
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> disponibilidadService.obtenerHorariosDisponibles(usuarioMock.getEmail(), requestMock));

        assertEquals("No se pueden crear citas en fechas pasadas", exception.getMessage());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando es día libre")
    void debeRetornarListaVacia_cuandoEsDiaLibre() {
        // Arrange
        DiaLibre diaLibre = DiaLibre.builder()
                .id("dia-libre-id")
                .fecha(requestMock.getFecha())
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.singletonList(diaLibre));

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        assertTrue(response.getHorariosDisponibles().isEmpty());
        assertEquals(30, response.getDuracionTotal());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay horario de trabajo configurado")
    void debeRetornarListaVacia_cuandoNoHayHorarioTrabajo() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        assertTrue(response.getHorariosDisponibles().isEmpty());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando el horario de trabajo está inactivo")
    void debeRetornarListaVacia_cuandoHorarioEstaInactivo() {
        // Arrange
        horarioTrabajoMock.setActivo(false);
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        assertTrue(response.getHorariosDisponibles().isEmpty());
    }

    @Test
    @DisplayName("Debe calcular duración total correctamente con múltiples servicios")
    void debeCalcularDuracionTotal_conMultiplesServicios() {
        // Arrange
        Servicio servicio2 = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Tinte")
                .duracionMinutos(60)
                .precio(BigDecimal.valueOf(300.0))
                .activo(true)
                .negocio(negocioMock)
                .build();

        requestMock.setServicioIds(Arrays.asList(
                servicioMock.getId().toString(),
                servicio2.getId().toString()
        ));

        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(servicioRepository.findById(servicio2.getId())).thenReturn(Optional.of(servicio2));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(Collections.emptyList());

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        assertEquals(90, response.getDuracionTotal()); // 30 + 60 = 90
    }

    @Test
    @DisplayName("Debe excluir horarios ocupados por citas existentes")
    void debeExcluirHorariosOcupados_porCitasExistentes() {
        // Arrange
        Cita citaExistente = Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(LocalDateTime.of(requestMock.getFecha(), LocalTime.of(10, 0)))
                .fechaFin(LocalDateTime.of(requestMock.getFecha(), LocalTime.of(11, 0)))
                .estado(Cita.EstadoCita.PENDIENTE)
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock))
                .thenReturn(Collections.singletonList(citaExistente));

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        // Verificar que los horarios entre 10:00 y 11:00 no están disponibles
        boolean tieneHorarioEnConflicto = response.getHorariosDisponibles().stream()
                .anyMatch(h -> h.getHoraInicio().equals(LocalTime.of(10, 0)));

        assertFalse(tieneHorarioEnConflicto);
    }

    @Test
    @DisplayName("Debe excluir citas canceladas del cálculo de disponibilidad")
    void debeExcluirCitasCanceladas() {
        // Arrange
        Cita citaCancelada = Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(LocalDateTime.of(requestMock.getFecha(), LocalTime.of(10, 0)))
                .fechaFin(LocalDateTime.of(requestMock.getFecha(), LocalTime.of(11, 0)))
                .estado(Cita.EstadoCita.CANCELADA)
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock))
                .thenReturn(Collections.singletonList(citaCancelada));

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        // Las citas canceladas no deberían bloquear horarios
        boolean tieneHorarioA10 = response.getHorariosDisponibles().stream()
                .anyMatch(h -> h.getHoraInicio().equals(LocalTime.of(10, 0)));

        assertTrue(tieneHorarioA10);
    }

    @Test
    @DisplayName("Debe marcar horarios entre 10:00 y 16:00 como recomendados")
    void debMarcarHorariosComoRecomendados() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(Collections.emptyList());

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        Optional<DisponibilidadResponse.HorarioDisponible> horarioRecomendado =
                response.getHorariosDisponibles().stream()
                        .filter(h -> h.getHoraInicio().equals(LocalTime.of(12, 0)))
                        .findFirst();

        assertTrue(horarioRecomendado.isPresent());
        assertTrue(horarioRecomendado.get().getRecomendado());

        Optional<DisponibilidadResponse.HorarioDisponible> horarioNoRecomendado =
                response.getHorariosDisponibles().stream()
                        .filter(h -> h.getHoraInicio().equals(LocalTime.of(9, 0)))
                        .findFirst();

        assertTrue(horarioNoRecomendado.isPresent());
        assertFalse(horarioNoRecomendado.get().getRecomendado());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando el servicio no existe")
    void debeLanzarNotFoundException_cuandoServicioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> disponibilidadService.obtenerHorariosDisponibles(usuarioMock.getEmail(), requestMock));

        assertTrue(exception.getMessage().contains("Servicio no encontrado"));
    }

    @Test
    @DisplayName("Debe lanzar BadRequestException cuando el servicio no pertenece al negocio")
    void debeLanzarBadRequestException_cuandoServicioNoPerteneceAlNegocio() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();
        servicioMock.setNegocio(otroNegocio);

        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> disponibilidadService.obtenerHorariosDisponibles(usuarioMock.getEmail(), requestMock));

        assertEquals("El servicio no pertenece a tu negocio", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar BadRequestException cuando el servicio está inactivo")
    void debeLanzarBadRequestException_cuandoServicioEstaInactivo() {
        // Arrange
        servicioMock.setActivo(false);
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> disponibilidadService.obtenerHorariosDisponibles(usuarioMock.getEmail(), requestMock));

        assertTrue(exception.getMessage().contains("no está activo"));
    }

    @Test
    @DisplayName("Debe excluir la cita especificada en citaIdExcluir")
    void debeExcluirCitaEspecificada() {
        // Arrange
        String citaIdExcluir = "cita-excluir-id";
        requestMock.setCitaIdExcluir(citaIdExcluir);

        Cita citaAExcluir = Cita.builder()
                .id(citaIdExcluir)
                .fechaHora(LocalDateTime.of(requestMock.getFecha(), LocalTime.of(10, 0)))
                .fechaFin(LocalDateTime.of(requestMock.getFecha(), LocalTime.of(11, 0)))
                .estado(Cita.EstadoCita.PENDIENTE)
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock))
                .thenReturn(Collections.singletonList(citaAExcluir));

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        // La cita excluida no debería bloquear el horario
        boolean tieneHorarioA10 = response.getHorariosDisponibles().stream()
                .anyMatch(h -> h.getHoraInicio().equals(LocalTime.of(10, 0)));

        assertTrue(tieneHorarioA10);
    }

    @Test
    @DisplayName("Debe generar intervalos de 15 minutos correctamente")
    void debeGenerarIntervalosCorrectamente() {
        // Arrange
        servicioMock.setDuracionMinutos(15); // Servicio de 15 minutos
        when(usuarioRepository.findByEmail(usuarioMock.getEmail())).thenReturn(Optional.of(usuarioMock));
        when(servicioRepository.findById(servicioMock.getId())).thenReturn(Optional.of(servicioMock));
        when(diaLibreRepository.findByNegocioAndFecha(negocioMock, requestMock.getFecha()))
                .thenReturn(Collections.emptyList());
        when(horarioTrabajoRepository.findByNegocioAndDiaSemana(any(Negocio.class), anyInt()))
                .thenReturn(Collections.singletonList(horarioTrabajoMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(Collections.emptyList());

        // Act
        DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(
                usuarioMock.getEmail(), requestMock);

        // Assert
        assertNotNull(response);
        assertFalse(response.getHorariosDisponibles().isEmpty());

        // Verificar que hay horarios cada 15 minutos
        boolean tieneHorario915 = response.getHorariosDisponibles().stream()
                .anyMatch(h -> h.getHoraInicio().equals(LocalTime.of(9, 15)));

        boolean tieneHorario930 = response.getHorariosDisponibles().stream()
                .anyMatch(h -> h.getHoraInicio().equals(LocalTime.of(9, 30)));

        assertTrue(tieneHorario915);
        assertTrue(tieneHorario930);
    }
}
