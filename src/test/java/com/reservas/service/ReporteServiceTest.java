package com.reservas.service;

import com.reservas.dto.response.ReporteResponse;
import com.reservas.entity.Cita;
import com.reservas.entity.Cliente;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.ClienteRepository;
import com.reservas.repository.UsuarioRepository;
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
@DisplayName("ReporteService - Pruebas Unitarias")
class ReporteServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private Cliente clienteMock;
    private Servicio servicioMock;
    private List<Cita> citasMock;

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

        clienteMock = Cliente.builder()
                .id("cliente-123")
                .nombre("María")
                .apellidoPaterno("González")
                .apellidoMaterno("López")
                .email("maria@cliente.com")
                .telefono("1234567890")
                .negocio(negocioMock)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        servicioMock = Servicio.builder()
                .id("servicio-123")
                .nombre("Corte de Cabello")
                .descripcion("Corte profesional")
                .precio(new BigDecimal("150.00"))
                .duracionMinutos(30)
                .activo(true)
                .negocio(negocioMock)
                .build();

        Servicio servicioMock2 = Servicio.builder()
                .id("servicio-456")
                .nombre("Tinte")
                .descripcion("Tinte profesional")
                .precio(new BigDecimal("300.00"))
                .duracionMinutos(60)
                .activo(true)
                .negocio(negocioMock)
                .build();

        LocalDateTime hoy = LocalDateTime.now();

        Cita cita1 = Cita.builder()
                .id("cita-1")
                .fechaHora(hoy.minusHours(2))
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .createdAt(hoy.minusHours(3))
                .build();

        Cita cita2 = Cita.builder()
                .id("cita-2")
                .fechaHora(hoy.plusHours(2))
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicioMock2)
                .negocio(negocioMock)
                .createdAt(hoy.minusHours(1))
                .build();

        Cita cita3 = Cita.builder()
                .id("cita-3")
                .fechaHora(hoy.plusHours(4))
                .estado(Cita.EstadoCita.CONFIRMADA)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .createdAt(hoy)
                .build();

        Cita cita4 = Cita.builder()
                .id("cita-4")
                .fechaHora(hoy.minusHours(5))
                .estado(Cita.EstadoCita.CANCELADA)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .createdAt(hoy.minusHours(6))
                .build();

        citasMock = Arrays.asList(cita1, cita2, cita3, cita4);
    }

    @Test
    @DisplayName("Generar reporte diario - Exitoso")
    void testGenerarReporteDiario_Exitoso() {
        // Arrange
        LocalDate fecha = LocalDate.now();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(citasMock);
        when(clienteRepository.findByNegocio(any())).thenReturn(Arrays.asList(clienteMock));

        // Act
        ReporteResponse response = reporteService.generarReporteDiario("usuario@test.com", fecha);

        // Assert
        assertNotNull(response);
        assertEquals("DIARIO", response.getPeriodo());
        assertEquals(4, response.getTotalCitas());
        assertEquals(1, response.getCitasPendientes());
        assertEquals(1, response.getCitasConfirmadas());
        assertEquals(1, response.getCitasCompletadas());
        assertEquals(1, response.getCitasCanceladas());
        assertEquals(new BigDecimal("150.00"), response.getIngresoTotal());
        assertEquals(new BigDecimal("450.00"), response.getIngresoEstimado());
        assertEquals(1, response.getClientesTotales());

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(citaRepository, times(1)).findByNegocioAndFechaHoraBetween(any(), any(), any());
    }

    @Test
    @DisplayName("Generar reporte diario - Usuario no encontrado")
    void testGenerarReporteDiario_UsuarioNoEncontrado() {
        // Arrange
        LocalDate fecha = LocalDate.now();
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            reporteService.generarReporteDiario("usuario@inexistente.com", fecha);
        });

        verify(citaRepository, never()).findByNegocioAndFechaHoraBetween(any(), any(), any());
    }

    @Test
    @DisplayName("Generar reporte diario - Negocio no encontrado")
    void testGenerarReporteDiario_NegocioNoEncontrado() {
        // Arrange
        Usuario usuarioSinNegocio = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@test.com")
                .negocio(null)
                .build();

        LocalDate fecha = LocalDate.now();
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioSinNegocio));

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            reporteService.generarReporteDiario("usuario@test.com", fecha);
        });
    }

    @Test
    @DisplayName("Generar reporte diario - Sin citas")
    void testGenerarReporteDiario_SinCitas() {
        // Arrange
        LocalDate fecha = LocalDate.now();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(clienteRepository.findByNegocio(any())).thenReturn(Arrays.asList(clienteMock));

        // Act
        ReporteResponse response = reporteService.generarReporteDiario("usuario@test.com", fecha);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalCitas());
        assertEquals(0, response.getCitasPendientes());
        assertEquals(0, response.getCitasConfirmadas());
        assertEquals(0, response.getCitasCompletadas());
        assertEquals(0, response.getCitasCanceladas());
        assertEquals(BigDecimal.ZERO, response.getIngresoTotal());
        assertEquals(BigDecimal.ZERO, response.getIngresoEstimado());
        assertNull(response.getServicioMasPopular());
    }

    @Test
    @DisplayName("Generar reporte semanal - Exitoso")
    void testGenerarReporteSemanal_Exitoso() {
        // Arrange
        LocalDate fechaInicio = LocalDate.now().minusDays(3);

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(citasMock);
        when(clienteRepository.findByNegocio(any())).thenReturn(Arrays.asList(clienteMock));

        // Act
        ReporteResponse response = reporteService.generarReporteSemanal("usuario@test.com", fechaInicio);

        // Assert
        assertNotNull(response);
        assertEquals("SEMANAL", response.getPeriodo());
        assertEquals(4, response.getTotalCitas());
        assertEquals(1, response.getCitasPendientes());
        assertEquals(1, response.getCitasConfirmadas());

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(citaRepository, times(1)).findByNegocioAndFechaHoraBetween(any(), any(), any());
    }

    @Test
    @DisplayName("Generar reporte mensual - Exitoso")
    void testGenerarReporteMensual_Exitoso() {
        // Arrange
        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(citasMock);
        when(clienteRepository.findByNegocio(any())).thenReturn(Arrays.asList(clienteMock));

        // Act
        ReporteResponse response = reporteService.generarReporteMensual("usuario@test.com", mes, anio);

        // Assert
        assertNotNull(response);
        assertEquals("MENSUAL", response.getPeriodo());
        assertEquals(4, response.getTotalCitas());

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(citaRepository, times(1)).findByNegocioAndFechaHoraBetween(any(), any(), any());
    }

    @Test
    @DisplayName("Generar reporte - Servicio más popular")
    void testGenerarReporte_ServicioMasPopular() {
        // Arrange
        LocalDate fecha = LocalDate.now();

        Servicio servicio1 = Servicio.builder()
                .id("serv-1")
                .nombre("Corte Popular")
                .precio(new BigDecimal("100.00"))
                .duracionMinutos(30)
                .build();

        Servicio servicio2 = Servicio.builder()
                .id("serv-2")
                .nombre("Tinte")
                .precio(new BigDecimal("200.00"))
                .duracionMinutos(60)
                .build();

        // 3 citas con servicio1, 1 cita con servicio2
        Cita cita1 = Cita.builder()
                .id("cita-1")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicio1)
                .negocio(negocioMock)
                .build();

        Cita cita2 = Cita.builder()
                .id("cita-2")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicio1)
                .negocio(negocioMock)
                .build();

        Cita cita3 = Cita.builder()
                .id("cita-3")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicio1)
                .negocio(negocioMock)
                .build();

        Cita cita4 = Cita.builder()
                .id("cita-4")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicio2)
                .negocio(negocioMock)
                .build();

        List<Cita> citasServicioPopular = Arrays.asList(cita1, cita2, cita3, cita4);

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(citasServicioPopular);
        when(clienteRepository.findByNegocio(any())).thenReturn(Arrays.asList(clienteMock));

        // Act
        ReporteResponse response = reporteService.generarReporteDiario("usuario@test.com", fecha);

        // Assert
        assertNotNull(response);
        assertEquals("Corte Popular", response.getServicioMasPopular());
        assertEquals(3, response.getServicioMasPopularCantidad());
    }

    @Test
    @DisplayName("Generar reporte - Clientes nuevos")
    void testGenerarReporte_ClientesNuevos() {
        // Arrange
        LocalDate fecha = LocalDate.now();
        LocalDateTime inicioHoy = fecha.atStartOfDay();

        Cliente clienteNuevo = Cliente.builder()
                .id("cliente-nuevo")
                .nombre("Cliente Nuevo")
                .apellidoPaterno("Test")
                .negocio(negocioMock)
                .createdAt(inicioHoy.plusHours(1)) // Creado hoy
                .build();

        Cliente clienteAntiguo = Cliente.builder()
                .id("cliente-antiguo")
                .nombre("Cliente Antiguo")
                .apellidoPaterno("Test")
                .negocio(negocioMock)
                .createdAt(inicioHoy.minusDays(10)) // Creado hace 10 días
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(clienteRepository.findByNegocio(any()))
                .thenReturn(Arrays.asList(clienteNuevo, clienteAntiguo));

        // Act
        ReporteResponse response = reporteService.generarReporteDiario("usuario@test.com", fecha);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getClientesTotales());
        assertEquals(1, response.getClientesNuevos());
    }

    @Test
    @DisplayName("Generar reporte - Cálculo de ingresos")
    void testGenerarReporte_CalculoIngresos() {
        // Arrange
        LocalDate fecha = LocalDate.now();

        Servicio servicio100 = Servicio.builder()
                .id("serv-100")
                .nombre("Servicio 100")
                .precio(new BigDecimal("100.00"))
                .duracionMinutos(30)
                .build();

        Servicio servicio200 = Servicio.builder()
                .id("serv-200")
                .nombre("Servicio 200")
                .precio(new BigDecimal("200.00"))
                .duracionMinutos(60)
                .build();

        // 2 citas completadas (ingresos reales) + 1 pendiente + 1 confirmada (ingresos estimados)
        Cita citaCompletada1 = Cita.builder()
                .id("cita-comp-1")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicio100) // +100
                .negocio(negocioMock)
                .build();

        Cita citaCompletada2 = Cita.builder()
                .id("cita-comp-2")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicio200) // +200
                .negocio(negocioMock)
                .build();

        Cita citaPendiente = Cita.builder()
                .id("cita-pend")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicio100) // +100 estimado
                .negocio(negocioMock)
                .build();

        Cita citaConfirmada = Cita.builder()
                .id("cita-conf")
                .fechaHora(LocalDateTime.now())
                .estado(Cita.EstadoCita.CONFIRMADA)
                .cliente(clienteMock)
                .servicio(servicio200) // +200 estimado
                .negocio(negocioMock)
                .build();

        List<Cita> citasIngresos = Arrays.asList(citaCompletada1, citaCompletada2, citaPendiente, citaConfirmada);

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(citaRepository.findByNegocioAndFechaHoraBetween(any(), any(), any()))
                .thenReturn(citasIngresos);
        when(clienteRepository.findByNegocio(any())).thenReturn(Arrays.asList(clienteMock));

        // Act
        ReporteResponse response = reporteService.generarReporteDiario("usuario@test.com", fecha);

        // Assert
        assertNotNull(response);
        // Ingresos reales: 100 + 200 = 300
        assertEquals(new BigDecimal("300.00"), response.getIngresoTotal());
        // Ingresos estimados: 100 (pendiente) + 200 (confirmada) = 300
        assertEquals(new BigDecimal("300.00"), response.getIngresoEstimado());
    }
}
