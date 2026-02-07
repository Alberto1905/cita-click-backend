package com.reservas.service;

import com.reservas.dto.response.DashboardMetricasResponse;
import com.reservas.entity.Cita;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.CitaRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardMetricasService - Pruebas Unitarias")
class DashboardMetricasServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @InjectMocks
    private DashboardMetricasService dashboardMetricasService;

    private Negocio negocioMock;
    private Usuario usuarioMock;
    private Servicio servicioMock;
    private String emailUsuario;

    @BeforeEach
    void setUp() {
        emailUsuario = "usuario@test.com";

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
                .email(emailUsuario)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .negocio(negocioMock)
                .build();

        servicioMock = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Corte de Cabello")
                .negocio(negocioMock)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Debe calcular métricas correctamente con datos completos")
    void debeCalcularMetricasCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        List<Cita> citas = crearCitasMock(ahora);

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getIngresos());
        assertNotNull(response.getCitas());
        assertNotNull(response.getServicios());
        assertNotNull(response.getTendenciaSemanal());
        assertEquals(7, response.getTendenciaSemanal().size());

        verify(usuarioRepository).findByEmail(emailUsuario);
        verify(citaRepository).findByNegocio(negocioMock);
        verify(servicioRepository).findByNegocio(negocioMock);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando usuario no existe")
    void debeLanzarNotFoundException_cuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
                dashboardMetricasService.obtenerMetricas(emailUsuario));

        verify(usuarioRepository).findByEmail(emailUsuario);
        verify(citaRepository, never()).findByNegocio(any());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando usuario no tiene negocio")
    void debeLanzarNotFoundException_cuandoUsuarioNoTieneNegocio() {
        // Arrange
        usuarioMock.setNegocio(null);
        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
                dashboardMetricasService.obtenerMetricas(emailUsuario));

        verify(usuarioRepository).findByEmail(emailUsuario);
        verify(citaRepository, never()).findByNegocio(any());
    }

    @Test
    @DisplayName("Debe calcular ingresos mensuales correctamente")
    void debeCalcularIngresosMensualesCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate inicioMes = ahora.toLocalDate().with(TemporalAdjusters.firstDayOfMonth());

        List<Cita> citas = Arrays.asList(
                crearCita(inicioMes.atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00")),
                crearCita(inicioMes.plusDays(5).atTime(14, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("150.00")),
                crearCita(inicioMes.plusDays(10).atTime(16, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("200.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals(new BigDecimal("450.00"), response.getIngresos().getIngresoMensual());
    }

    @Test
    @DisplayName("Debe calcular ingresos semanales correctamente")
    void debeCalcularIngresosSemanalCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate inicioSemana = ahora.toLocalDate().with(java.time.DayOfWeek.MONDAY);

        List<Cita> citas = Arrays.asList(
                crearCita(inicioSemana.atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00")),
                crearCita(inicioSemana.plusDays(2).atTime(14, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("150.00")),
                crearCita(inicioSemana.minusDays(7).atTime(16, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("200.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals(new BigDecimal("250.00"), response.getIngresos().getIngresoSemanal());
    }

    @Test
    @DisplayName("Debe calcular ingreso diario promedio correctamente")
    void debeCalcularIngresoDiarioPromedioCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate inicioMes = ahora.toLocalDate().with(TemporalAdjusters.firstDayOfMonth());

        List<Cita> citas = Arrays.asList(
                crearCita(inicioMes.atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("300.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        int diasTranscurridos = ahora.toLocalDate().getDayOfMonth();
        BigDecimal esperado = new BigDecimal("300.00")
                .divide(BigDecimal.valueOf(diasTranscurridos), 2, RoundingMode.HALF_UP);
        assertEquals(esperado, response.getIngresos().getIngresoDiarioPromedio());
    }

    @Test
    @DisplayName("Debe calcular diferencia con mes anterior correctamente")
    void debeCalcularDiferenciaMesAnteriorCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate inicioMes = ahora.toLocalDate().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);

        List<Cita> citas = Arrays.asList(
                // Mes actual: 200
                crearCita(inicioMes.atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("200.00")),
                // Mes anterior: 100
                crearCita(inicioMesAnterior.atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        // (200 - 100) / 100 * 100 = 100%
        assertEquals(new BigDecimal("100.0000"), response.getIngresos().getDiferenciaMesAnterior());
    }

    @Test
    @DisplayName("Debe contar citas del mes correctamente excluyendo canceladas")
    void debeContarCitasMesCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate inicioMes = ahora.toLocalDate().with(TemporalAdjusters.firstDayOfMonth());

        List<Cita> citas = Arrays.asList(
                crearCita(inicioMes.atTime(10, 0), Cita.EstadoCita.PENDIENTE, new BigDecimal("100.00")),
                crearCita(inicioMes.plusDays(5).atTime(14, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("150.00")),
                crearCita(inicioMes.plusDays(10).atTime(16, 0), Cita.EstadoCita.CANCELADA, new BigDecimal("200.00")),
                crearCita(inicioMes.minusMonths(1).atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("50.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals(2L, response.getCitas().getTotalMes());
    }

    @Test
    @DisplayName("Debe contar citas de hoy correctamente")
    void debeContarCitasHoyCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();

        List<Cita> citas = Arrays.asList(
                crearCita(ahora.withHour(10), Cita.EstadoCita.PENDIENTE, new BigDecimal("100.00")),
                crearCita(ahora.withHour(14), Cita.EstadoCita.COMPLETADA, new BigDecimal("150.00")),
                crearCita(ahora.minusDays(1).withHour(10), Cita.EstadoCita.COMPLETADA, new BigDecimal("200.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals(2L, response.getCitas().getTotalHoy());
    }

    @Test
    @DisplayName("Debe calcular día de mayor demanda correctamente")
    void debeCalcularDiaMayorDemandaCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hace15Dias = ahora.toLocalDate().minusDays(15);

        List<Cita> citas = Arrays.asList(
                crearCita(hace15Dias.with(java.time.DayOfWeek.MONDAY).atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00")),
                crearCita(hace15Dias.with(java.time.DayOfWeek.MONDAY).atTime(14, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00")),
                crearCita(hace15Dias.with(java.time.DayOfWeek.TUESDAY).atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals("lunes", response.getCitas().getDiaMayorDemanda().toLowerCase());
    }

    @Test
    @DisplayName("Debe calcular hora de mayor demanda correctamente")
    void debeCalcularHoraMayorDemandaCorrectamente() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hace15Dias = ahora.toLocalDate().minusDays(15);

        List<Cita> citas = Arrays.asList(
                crearCita(hace15Dias.atTime(14, 30), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00")),
                crearCita(hace15Dias.atTime(14, 45), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00")),
                crearCita(hace15Dias.atTime(10, 0), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals("14:00", response.getCitas().getHoraMayorDemanda());
    }

    @Test
    @DisplayName("Debe contar servicios activos correctamente")
    void debeContarServiciosActivosCorrectamente() {
        // Arrange
        Servicio servicio1 = Servicio.builder().id(UUID.randomUUID()).nombre("Servicio 1").negocio(negocioMock).activo(true).build();
        Servicio servicio2 = Servicio.builder().id(UUID.randomUUID()).nombre("Servicio 2").negocio(negocioMock).activo(true).build();
        Servicio servicio3 = Servicio.builder().id(UUID.randomUUID()).nombre("Servicio 3").negocio(negocioMock).activo(false).build();

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(new ArrayList<>());
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicio1, servicio2, servicio3));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals(2, response.getServicios().getTotalServiciosActivos());
    }

    @Test
    @DisplayName("Debe calcular servicios más solicitados correctamente")
    void debeCalcularServiciosMasSolicitadosCorrectamente() {
        // Arrange
        Servicio servicio1 = Servicio.builder().id(UUID.randomUUID()).nombre("Servicio Popular").negocio(negocioMock).activo(true).build();
        Servicio servicio2 = Servicio.builder().id(UUID.randomUUID()).nombre("Servicio Normal").negocio(negocioMock).activo(true).build();

        List<Cita> citas = Arrays.asList(
                crearCitaConServicio(LocalDateTime.now(), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"), servicio1),
                crearCitaConServicio(LocalDateTime.now(), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"), servicio1),
                crearCitaConServicio(LocalDateTime.now(), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"), servicio1),
                crearCitaConServicio(LocalDateTime.now(), Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"), servicio2)
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicio1, servicio2));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertFalse(response.getServicios().getServiciosMasSolicitados().isEmpty());
        assertEquals("Servicio Popular", response.getServicios().getServiciosMasSolicitados().get(0).getNombre());
        assertEquals(3L, response.getServicios().getServiciosMasSolicitados().get(0).getCantidadCitas());
        assertEquals(new BigDecimal("300.00"), response.getServicios().getServiciosMasSolicitados().get(0).getIngresoGenerado());
    }

    @Test
    @DisplayName("Debe generar tendencia semanal de 7 días")
    void debeGenerarTendenciaSemanal7Dias() {
        // Arrange
        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(new ArrayList<>());
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertEquals(7, response.getTendenciaSemanal().size());

        // Verificar que las fechas están en orden
        for (int i = 0; i < response.getTendenciaSemanal().size() - 1; i++) {
            LocalDate fechaActual = response.getTendenciaSemanal().get(i).getFecha();
            LocalDate fechaSiguiente = response.getTendenciaSemanal().get(i + 1).getFecha();
            assertTrue(fechaActual.isBefore(fechaSiguiente));
        }
    }

    @Test
    @DisplayName("Debe manejar correctamente lista vacía de citas")
    void debeManejarListaVaciaCitas() {
        // Arrange
        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(new ArrayList<>());
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(new ArrayList<>());

        // Act
        DashboardMetricasResponse response = dashboardMetricasService.obtenerMetricas(emailUsuario);

        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getIngresos().getIngresoMensual());
        assertEquals(0L, response.getCitas().getTotalMes());
        assertEquals("N/A", response.getCitas().getDiaMayorDemanda());
        assertEquals("N/A", response.getCitas().getHoraMayorDemanda());
        assertTrue(response.getServicios().getServiciosMasSolicitados().isEmpty());
    }

    @Test
    @DisplayName("Debe manejar citas sin precio (null)")
    void debeManejarCitasSinPrecio() {
        // Arrange
        LocalDateTime ahora = LocalDateTime.now();
        List<Cita> citas = Arrays.asList(
                crearCita(ahora, Cita.EstadoCita.COMPLETADA, null),
                crearCita(ahora, Cita.EstadoCita.COMPLETADA, new BigDecimal("100.00"))
        );

        when(usuarioRepository.findByEmail(emailUsuario)).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);
        when(servicioRepository.findByNegocio(negocioMock)).thenReturn(Arrays.asList(servicioMock));

        // Act & Assert - No debe lanzar NullPointerException
        assertDoesNotThrow(() -> dashboardMetricasService.obtenerMetricas(emailUsuario));
    }

    // Métodos auxiliares

    private List<Cita> crearCitasMock(LocalDateTime base) {
        List<Cita> citas = new ArrayList<>();
        LocalDate inicioMes = base.toLocalDate().with(TemporalAdjusters.firstDayOfMonth());

        for (int i = 0; i < 5; i++) {
            citas.add(crearCita(
                    inicioMes.plusDays(i).atTime(10, 0),
                    Cita.EstadoCita.COMPLETADA,
                    new BigDecimal("100.00")
            ));
        }

        return citas;
    }

    private Cita crearCita(LocalDateTime fechaHora, Cita.EstadoCita estado, BigDecimal precio) {
        return Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(fechaHora)
                .fechaFin(fechaHora.plusHours(1))
                .estado(estado)
                .precio(precio)
                .negocio(negocioMock)
                .servicio(servicioMock)
                .build();
    }

    private Cita crearCitaConServicio(LocalDateTime fechaHora, Cita.EstadoCita estado, BigDecimal precio, Servicio servicio) {
        return Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(fechaHora)
                .fechaFin(fechaHora.plusHours(1))
                .estado(estado)
                .precio(precio)
                .negocio(negocioMock)
                .servicio(servicio)
                .build();
    }
}
