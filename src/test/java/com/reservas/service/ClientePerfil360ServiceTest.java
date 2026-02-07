package com.reservas.service;

import com.reservas.dto.response.ClientePerfil360Response;
import com.reservas.entity.*;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientePerfil360Service - Pruebas Unitarias")
class ClientePerfil360ServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ClientePerfil360Service clientePerfil360Service;

    private Negocio negocioMock;
    private Usuario usuarioMock;
    private Cliente clienteMock;
    private Servicio servicioMock;
    private Cita citaMock;
    private UUID clienteId;
    private String email;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        email = "usuario@test.com";

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
                .email(email)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .rol("owner")
                .negocio(negocioMock)
                .build();

        clienteMock = Cliente.builder()
                .id(clienteId)
                .nombre("María")
                .apellidoPaterno("González")
                .apellidoMaterno("López")
                .email("maria@test.com")
                .telefono("+525512345678")
                .notas("Cliente VIP")
                .negocio(negocioMock)
                .build();

        // Set audit fields manually using setters (since @CreatedDate doesn't work in tests)
        clienteMock.setCreatedAt(LocalDateTime.now().minusMonths(6));
        clienteMock.setUpdatedAt(LocalDateTime.now());

        servicioMock = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Corte de Cabello")
                .negocio(negocioMock)
                .build();

        citaMock = Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(LocalDateTime.now().minusDays(5))
                .fechaFin(LocalDateTime.now().minusDays(5).plusHours(1))
                .estado(Cita.EstadoCita.COMPLETADA)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .precio(new BigDecimal("250.00"))
                .esRecurrente(false)
                .build();
    }

    @Test
    @DisplayName("Debe obtener perfil completo del cliente con todas las secciones")
    void debeObtenerPerfilCompletoDelCliente() {
        // Arrange
        List<Cita> citas = Arrays.asList(citaMock);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        assertNotNull(response);
        assertEquals(clienteId, response.getId());
        assertEquals("María", response.getNombre());
        assertEquals("González", response.getApellidoPaterno());
        assertEquals("López", response.getApellidoMaterno());
        assertEquals("María González López", response.getNombreCompleto());
        assertEquals("maria@test.com", response.getEmail());
        assertEquals("+525512345678", response.getTelefono());
        assertEquals("Cliente VIP", response.getNotas());
        assertNotNull(response.getEstadisticas());
        assertNotNull(response.getHistorialCitas());
        assertNotNull(response.getProximasCitas());
        assertNotNull(response.getServiciosFrecuentes());

        verify(usuarioRepository).findByEmail(email);
        verify(clienteRepository).findById(clienteId);
        verify(citaRepository).findByNegocio(negocioMock);
    }

    @Test
    @DisplayName("Debe construir nombre completo sin apellido materno si es null")
    void debeConstruirNombreCompletoSinApellidoMaterno() {
        // Arrange
        clienteMock.setApellidoMaterno(null);
        List<Cita> citas = Arrays.asList(citaMock);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        assertEquals("María González", response.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe construir nombre completo sin apellido materno si está vacío")
    void debeConstruirNombreCompletoSinApellidoMaternoVacio() {
        // Arrange
        clienteMock.setApellidoMaterno("");
        List<Cita> citas = Arrays.asList(citaMock);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        assertEquals("María González", response.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe calcular estadísticas correctamente con citas completadas")
    void debeCalcularEstadisticasConCitasCompletadas() {
        // Arrange
        Cita cita1 = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(10), new BigDecimal("250.00"));
        Cita cita2 = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(5), new BigDecimal("300.00"));
        Cita cita3 = createCita(Cita.EstadoCita.CANCELADA, LocalDateTime.now().minusDays(3), new BigDecimal("200.00"));
        List<Cita> citas = Arrays.asList(cita1, cita2, cita3);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        ClientePerfil360Response.EstadisticasCitas stats = response.getEstadisticas();
        assertEquals(3L, stats.getTotalCitas());
        assertEquals(2L, stats.getCitasCompletadas());
        assertEquals(1L, stats.getCitasCanceladas());
        assertEquals(0L, stats.getCitasPendientes());
        assertEquals(0L, stats.getCitasConfirmadas());
        assertEquals(new BigDecimal("550.00"), stats.getGastoTotal());
        assertEquals(new BigDecimal("275.00"), stats.getGastoPromedio());
        assertNotNull(stats.getUltimaCita());
    }

    @Test
    @DisplayName("Debe calcular estadísticas con diferentes estados de citas")
    void debeCalcularEstadisticasConDiferentesEstados() {
        // Arrange
        Cita citaCompletada = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(2), new BigDecimal("250.00"));
        Cita citaPendiente = createCita(Cita.EstadoCita.PENDIENTE, LocalDateTime.now().plusDays(2), new BigDecimal("200.00"));
        Cita citaConfirmada = createCita(Cita.EstadoCita.CONFIRMADA, LocalDateTime.now().plusDays(5), new BigDecimal("300.00"));
        Cita citaCancelada = createCita(Cita.EstadoCita.CANCELADA, LocalDateTime.now().minusDays(1), new BigDecimal("150.00"));
        List<Cita> citas = Arrays.asList(citaCompletada, citaPendiente, citaConfirmada, citaCancelada);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        ClientePerfil360Response.EstadisticasCitas stats = response.getEstadisticas();
        assertEquals(4L, stats.getTotalCitas());
        assertEquals(1L, stats.getCitasCompletadas());
        assertEquals(1L, stats.getCitasCanceladas());
        assertEquals(1L, stats.getCitasPendientes());
        assertEquals(1L, stats.getCitasConfirmadas());
        assertEquals(new BigDecimal("250.00"), stats.getGastoTotal()); // Solo citas completadas
        assertEquals(new BigDecimal("250.00"), stats.getGastoPromedio());
    }

    @Test
    @DisplayName("Debe calcular última cita correctamente (solo completadas/confirmadas pasadas)")
    void debeCalcularUltimaCitaCorrectamente() {
        // Arrange
        Cita citaPasada1 = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(10), new BigDecimal("200.00"));
        Cita citaPasada2 = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(3), new BigDecimal("250.00"));
        Cita citaFutura = createCita(Cita.EstadoCita.CONFIRMADA, LocalDateTime.now().plusDays(2), new BigDecimal("300.00"));
        List<Cita> citas = Arrays.asList(citaPasada1, citaPasada2, citaFutura);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        LocalDateTime ultimaCita = response.getEstadisticas().getUltimaCita();
        assertNotNull(ultimaCita);
        assertTrue(ultimaCita.isBefore(LocalDateTime.now()));
        assertEquals(citaPasada2.getFechaHora(), ultimaCita);
    }

    @Test
    @DisplayName("Debe calcular próxima cita correctamente (solo futuras confirmadas/pendientes)")
    void debeCalcularProximaCitaCorrectamente() {
        // Arrange
        Cita citaPasada = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(5), new BigDecimal("200.00"));
        Cita citaFutura1 = createCita(Cita.EstadoCita.CONFIRMADA, LocalDateTime.now().plusDays(3), new BigDecimal("250.00"));
        Cita citaFutura2 = createCita(Cita.EstadoCita.PENDIENTE, LocalDateTime.now().plusDays(7), new BigDecimal("300.00"));
        List<Cita> citas = Arrays.asList(citaPasada, citaFutura1, citaFutura2);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        LocalDateTime proximaCita = response.getEstadisticas().getProximaCita();
        assertNotNull(proximaCita);
        assertTrue(proximaCita.isAfter(LocalDateTime.now()));
        assertEquals(citaFutura1.getFechaHora(), proximaCita);
    }

    @Test
    @DisplayName("Debe manejar cliente sin citas correctamente")
    void debeManejarClienteSinCitas() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(new ArrayList<>());

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        assertNotNull(response);
        ClientePerfil360Response.EstadisticasCitas stats = response.getEstadisticas();
        assertEquals(0L, stats.getTotalCitas());
        assertEquals(0L, stats.getCitasCompletadas());
        assertEquals(BigDecimal.ZERO, stats.getGastoTotal());
        assertEquals(BigDecimal.ZERO, stats.getGastoPromedio());
        assertNull(stats.getUltimaCita());
        assertNull(stats.getProximaCita());
        assertTrue(response.getHistorialCitas().isEmpty());
        assertTrue(response.getProximasCitas().isEmpty());
        assertTrue(response.getServiciosFrecuentes().isEmpty());
    }

    @Test
    @DisplayName("Debe calcular servicios más frecuentes correctamente")
    void debeCalcularServiciosFrecuentesCorrectamente() {
        // Arrange
        Servicio servicio1 = Servicio.builder().id(UUID.randomUUID()).nombre("Corte").negocio(negocioMock).build();
        Servicio servicio2 = Servicio.builder().id(UUID.randomUUID()).nombre("Tinte").negocio(negocioMock).build();

        Cita cita1 = createCitaWithService(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(10), new BigDecimal("250.00"), servicio1);
        Cita cita2 = createCitaWithService(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(8), new BigDecimal("250.00"), servicio1);
        Cita cita3 = createCitaWithService(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(5), new BigDecimal("500.00"), servicio2);
        List<Cita> citas = Arrays.asList(cita1, cita2, cita3);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        List<ClientePerfil360Response.ServicioUtilizado> servicios = response.getServiciosFrecuentes();
        assertEquals(2, servicios.size());

        // Primero debe estar el servicio más usado (Corte - 2 veces)
        ClientePerfil360Response.ServicioUtilizado servicioTop = servicios.get(0);
        assertEquals("Corte", servicioTop.getServicioNombre());
        assertEquals(2L, servicioTop.getCantidadVeces());
        assertEquals(new BigDecimal("500.00"), servicioTop.getGastoTotal());

        // Segundo debe estar Tinte (1 vez)
        ClientePerfil360Response.ServicioUtilizado servicioSegundo = servicios.get(1);
        assertEquals("Tinte", servicioSegundo.getServicioNombre());
        assertEquals(1L, servicioSegundo.getCantidadVeces());
        assertEquals(new BigDecimal("500.00"), servicioSegundo.getGastoTotal());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando usuario no existe")
    void debeLanzarNotFoundExceptionCuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            clientePerfil360Service.obtenerPerfil360(email, clienteId.toString())
        );

        verify(usuarioRepository).findByEmail(email);
        verify(clienteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando negocio no existe")
    void debeLanzarNotFoundExceptionCuandoNegocioNoExiste() {
        // Arrange
        usuarioMock.setNegocio(null);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            clientePerfil360Service.obtenerPerfil360(email, clienteId.toString())
        );

        verify(clienteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando cliente no existe")
    void debeLanzarNotFoundExceptionCuandoClienteNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            clientePerfil360Service.obtenerPerfil360(email, clienteId.toString())
        );

        verify(usuarioRepository).findByEmail(email);
        verify(clienteRepository).findById(clienteId);
        verify(citaRepository, never()).findByNegocio(any());
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedException cuando cliente no pertenece al negocio")
    void debeLanzarUnauthorizedExceptionCuandoClienteNoPertenece() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();
        clienteMock.setNegocio(otroNegocio);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
            clientePerfil360Service.obtenerPerfil360(email, clienteId.toString())
        );

        verify(citaRepository, never()).findByNegocio(any());
    }

    @Test
    @DisplayName("Debe construir historial de citas ordenado por fecha descendente")
    void debeConstruirHistorialOrdenadoPorFecha() {
        // Arrange
        Cita cita1 = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(10), new BigDecimal("200.00"));
        Cita cita2 = createCita(Cita.EstadoCita.COMPLETADA, LocalDateTime.now().minusDays(5), new BigDecimal("250.00"));
        Cita cita3 = createCita(Cita.EstadoCita.CANCELADA, LocalDateTime.now().minusDays(2), new BigDecimal("300.00"));
        List<Cita> citas = Arrays.asList(cita1, cita2, cita3);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        List<ClientePerfil360Response.CitaResumen> historial = response.getHistorialCitas();
        assertEquals(3, historial.size());
        // Debe estar ordenado de más reciente a más antigua
        assertTrue(LocalDateTime.parse(historial.get(0).getFechaHora().toString())
                .isAfter(LocalDateTime.parse(historial.get(1).getFechaHora().toString())));
        assertTrue(LocalDateTime.parse(historial.get(1).getFechaHora().toString())
                .isAfter(LocalDateTime.parse(historial.get(2).getFechaHora().toString())));
    }

    @Test
    @DisplayName("Debe construir próximas citas ordenadas por fecha ascendente")
    void debeConstruirProximasCitasOrdenadasPorFecha() {
        // Arrange
        Cita cita1 = createCita(Cita.EstadoCita.CONFIRMADA, LocalDateTime.now().plusDays(7), new BigDecimal("200.00"));
        Cita cita2 = createCita(Cita.EstadoCita.PENDIENTE, LocalDateTime.now().plusDays(3), new BigDecimal("250.00"));
        Cita cita3 = createCita(Cita.EstadoCita.CONFIRMADA, LocalDateTime.now().plusDays(10), new BigDecimal("300.00"));
        List<Cita> citas = Arrays.asList(cita1, cita2, cita3);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteMock));
        when(citaRepository.findByNegocio(negocioMock)).thenReturn(citas);

        // Act
        ClientePerfil360Response response = clientePerfil360Service.obtenerPerfil360(email, clienteId.toString());

        // Assert
        List<ClientePerfil360Response.CitaResumen> proximas = response.getProximasCitas();
        assertEquals(3, proximas.size());
        // Debe estar ordenado de más cercana a más lejana
        assertTrue(proximas.get(0).getFechaHora()
                .isBefore(proximas.get(1).getFechaHora()));
        assertTrue(proximas.get(1).getFechaHora()
                .isBefore(proximas.get(2).getFechaHora()));
    }

    // Helper methods
    private Cita createCita(Cita.EstadoCita estado, LocalDateTime fechaHora, BigDecimal precio) {
        return Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(fechaHora)
                .fechaFin(fechaHora.plusHours(1))
                .estado(estado)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .precio(precio)
                .esRecurrente(false)
                .build();
    }

    private Cita createCitaWithService(Cita.EstadoCita estado, LocalDateTime fechaHora, BigDecimal precio, Servicio servicio) {
        return Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(fechaHora)
                .fechaFin(fechaHora.plusHours(1))
                .estado(estado)
                .cliente(clienteMock)
                .servicio(servicio)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .precio(precio)
                .esRecurrente(false)
                .build();
    }
}
