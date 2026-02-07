package com.reservas.service;

import com.reservas.entity.Cita;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.TipoRecurrencia;
import com.reservas.entity.Usuario;
import com.reservas.entity.Cliente;
import com.reservas.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitaRecurrenteService - Pruebas Unitarias")
class CitaRecurrenteServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @InjectMocks
    private CitaRecurrenteService citaRecurrenteService;

    private Negocio negocioMock;
    private Cliente clienteMock;
    private Servicio servicioMock;
    private Usuario usuarioMock;
    private Cita citaPadreMock;

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

        clienteMock = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("María")
                .apellidoPaterno("González")
                .email("maria@test.com")
                .telefono("+525512345678")
                .negocio(negocioMock)
                .build();

        servicioMock = Servicio.builder()
                .id(UUID.randomUUID())
                .nombre("Corte de Cabello")
                .negocio(negocioMock)
                .build();

        citaPadreMock = Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(LocalDateTime.now().plusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1).plusHours(1))
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .precio(new BigDecimal("150.00"))
                .notas("Notas de prueba")
                .esRecurrente(true)
                .tipoRecurrencia(TipoRecurrencia.SEMANAL)
                .numeroOcurrencias(5)
                .build();
    }

    @Test
    @DisplayName("Debe generar citas recurrentes diarias correctamente")
    void debeGenerarCitasRecurrentesDiarias() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.DIARIA);
        citaPadreMock.setNumeroOcurrencias(5);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(5, citasGeneradas.size());
        verify(citaRepository).saveAll(anyList());

        // Verificar que las fechas están espaciadas por 1 día
        for (int i = 0; i < citasGeneradas.size() - 1; i++) {
            LocalDateTime fechaActual = citasGeneradas.get(i).getFechaHora();
            LocalDateTime fechaSiguiente = citasGeneradas.get(i + 1).getFechaHora();
            assertEquals(1, java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaSiguiente));
        }
    }

    @Test
    @DisplayName("Debe generar citas recurrentes semanales correctamente")
    void debeGenerarCitasRecurrentesSemanales() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.SEMANAL);
        citaPadreMock.setNumeroOcurrencias(4);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(4, citasGeneradas.size());
        verify(citaRepository).saveAll(anyList());

        // Verificar que las fechas están espaciadas por 1 semana
        for (int i = 0; i < citasGeneradas.size() - 1; i++) {
            LocalDateTime fechaActual = citasGeneradas.get(i).getFechaHora();
            LocalDateTime fechaSiguiente = citasGeneradas.get(i + 1).getFechaHora();
            assertEquals(7, java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaSiguiente));
        }
    }

    @Test
    @DisplayName("Debe generar citas recurrentes quincenales correctamente")
    void debeGenerarCitasRecurrentesQuincenales() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.QUINCENAL);
        citaPadreMock.setNumeroOcurrencias(3);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(3, citasGeneradas.size());

        // Verificar que las fechas están espaciadas por 2 semanas (14 días)
        for (int i = 0; i < citasGeneradas.size() - 1; i++) {
            LocalDateTime fechaActual = citasGeneradas.get(i).getFechaHora();
            LocalDateTime fechaSiguiente = citasGeneradas.get(i + 1).getFechaHora();
            assertEquals(14, java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaSiguiente));
        }
    }

    @Test
    @DisplayName("Debe generar citas recurrentes mensuales correctamente")
    void debeGenerarCitasRecurrentesMensuales() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.MENSUAL);
        citaPadreMock.setNumeroOcurrencias(3);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(3, citasGeneradas.size());

        // Verificar que las fechas están espaciadas aproximadamente por 1 mes
        for (int i = 0; i < citasGeneradas.size() - 1; i++) {
            LocalDateTime fechaActual = citasGeneradas.get(i).getFechaHora();
            LocalDateTime fechaSiguiente = citasGeneradas.get(i + 1).getFechaHora();
            long diasEntreFechas = java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaSiguiente);
            assertTrue(diasEntreFechas >= 28 && diasEntreFechas <= 31);
        }
    }

    @Test
    @DisplayName("Debe generar citas recurrentes trimestrales correctamente")
    void debeGenerarCitasRecurrentesTrimestrales() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.TRIMESTRAL);
        citaPadreMock.setNumeroOcurrencias(2);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(2, citasGeneradas.size());

        // Verificar que las fechas están espaciadas aproximadamente por 3 meses
        LocalDateTime fechaPrimera = citasGeneradas.get(0).getFechaHora();
        LocalDateTime fechaSegunda = citasGeneradas.get(1).getFechaHora();
        long diasEntreFechas = java.time.temporal.ChronoUnit.DAYS.between(fechaPrimera, fechaSegunda);
        assertTrue(diasEntreFechas >= 89 && diasEntreFechas <= 92);
    }

    @Test
    @DisplayName("Debe generar citas personalizadas con intervalo específico")
    void debeGenerarCitasPersonalizadas() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.PERSONALIZADO);
        citaPadreMock.setIntervaloRecurrencia(3); // Cada 3 días
        citaPadreMock.setNumeroOcurrencias(4);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(4, citasGeneradas.size());

        // Verificar que las fechas están espaciadas por 3 días
        for (int i = 0; i < citasGeneradas.size() - 1; i++) {
            LocalDateTime fechaActual = citasGeneradas.get(i).getFechaHora();
            LocalDateTime fechaSiguiente = citasGeneradas.get(i + 1).getFechaHora();
            assertEquals(3, java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaSiguiente));
        }
    }

    @Test
    @DisplayName("Debe generar citas semanales en días específicos")
    void debeGenerarCitasSemanalDiasEspecificos() {
        // Arrange
        // Configurar cita padre para lunes
        LocalDateTime proximoLunes = LocalDateTime.now().with(DayOfWeek.MONDAY).plusWeeks(1).withHour(10).withMinute(0);
        citaPadreMock.setFechaHora(proximoLunes);
        citaPadreMock.setFechaFin(proximoLunes.plusHours(1));
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.SEMANAL);
        citaPadreMock.setDiasSemana("LUN,MIE,VIE"); // Lunes, Miércoles, Viernes
        citaPadreMock.setNumeroOcurrencias(6);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(6, citasGeneradas.size());

        // Verificar que solo se generan en los días especificados
        for (Cita cita : citasGeneradas) {
            DayOfWeek diaSemana = cita.getFechaHora().getDayOfWeek();
            assertTrue(diaSemana == DayOfWeek.MONDAY ||
                      diaSemana == DayOfWeek.WEDNESDAY ||
                      diaSemana == DayOfWeek.FRIDAY);
        }
    }

    @Test
    @DisplayName("Debe respetar fecha límite de recurrencia")
    void debeRespetarFechaLimiteRecurrencia() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.DIARIA);
        citaPadreMock.setNumeroOcurrencias(100); // Número alto
        citaPadreMock.setFechaFinRecurrencia(LocalDateTime.now().plusDays(10)); // Límite de 10 días

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertTrue(citasGeneradas.size() < 100);
        assertTrue(citasGeneradas.size() <= 10);

        // Verificar que ninguna cita excede la fecha límite
        for (Cita cita : citasGeneradas) {
            assertTrue(cita.getFechaHora().isBefore(citaPadreMock.getFechaFinRecurrencia()));
        }
    }

    @Test
    @DisplayName("Debe limitar a 52 ocurrencias por defecto")
    void debeLimitarA52OcurrenciasPorDefecto() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.DIARIA);
        citaPadreMock.setNumeroOcurrencias(null); // Sin límite específico
        citaPadreMock.setFechaFinRecurrencia(null); // Sin fecha límite

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertEquals(52, citasGeneradas.size());
    }

    @Test
    @DisplayName("No debe generar citas si no es recurrente")
    void noDebeGenerarCitas_siNoEsRecurrente() {
        // Arrange
        citaPadreMock.setEsRecurrente(false);

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertTrue(citasGeneradas.isEmpty());
        verify(citaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("No debe generar citas si tipo es NO_RECURRENTE")
    void noDebeGenerarCitas_siTipoNoRecurrente() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.NO_RECURRENTE);

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        assertTrue(citasGeneradas.isEmpty());
        verify(citaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe copiar correctamente atributos de cita padre a hijas")
    void debeCopiarAtributosCitaPadre() {
        // Arrange
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.DIARIA);
        citaPadreMock.setNumeroOcurrencias(2);

        ArgumentCaptor<List<Cita>> captor = ArgumentCaptor.forClass(List.class);
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        verify(citaRepository).saveAll(captor.capture());
        List<Cita> citasGuardadas = captor.getValue();

        for (Cita citaHija : citasGuardadas) {
            assertEquals(citaPadreMock.getNegocio(), citaHija.getNegocio());
            assertEquals(citaPadreMock.getCliente(), citaHija.getCliente());
            assertEquals(citaPadreMock.getUsuario(), citaHija.getUsuario());
            assertEquals(citaPadreMock.getServicio(), citaHija.getServicio());
            assertEquals(citaPadreMock.getPrecio(), citaHija.getPrecio());
            assertEquals(citaPadreMock.getNotas(), citaHija.getNotas());
            assertEquals(Cita.EstadoCita.PENDIENTE, citaHija.getEstado());
            assertEquals(citaPadreMock.getId(), citaHija.getCitaPadreId());
            assertFalse(citaHija.isEsRecurrente());
            assertEquals(TipoRecurrencia.NO_RECURRENTE, citaHija.getTipoRecurrencia());
        }
    }

    @Test
    @DisplayName("Debe mantener duración de cita en citas hijas")
    void debeMantenerDuracionCita() {
        // Arrange
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusHours(2).plusMinutes(30); // Duración: 2.5 horas
        citaPadreMock.setFechaHora(inicio);
        citaPadreMock.setFechaFin(fin);
        citaPadreMock.setTipoRecurrencia(TipoRecurrencia.DIARIA);
        citaPadreMock.setNumeroOcurrencias(3);

        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(citaPadreMock);

        // Assert
        for (Cita cita : citasGeneradas) {
            long duracion = java.time.temporal.ChronoUnit.MINUTES.between(cita.getFechaHora(), cita.getFechaFin());
            assertEquals(150, duracion); // 2.5 horas = 150 minutos
        }
    }

    @Test
    @DisplayName("Debe cancelar serie recurrente completa")
    void debeCancelarSerieRecurrenteCompleta() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        List<Cita> citasHijas = Arrays.asList(
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(1)),
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(2)),
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(3))
        );

        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(citasHijas);
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int canceladas = citaRecurrenteService.cancelarSerieRecurrente(citaPadreId);

        // Assert
        assertEquals(3, canceladas);
        verify(citaRepository).saveAll(anyList());

        // Verificar que todas fueron marcadas como canceladas
        for (Cita cita : citasHijas) {
            assertEquals(Cita.EstadoCita.CANCELADA, cita.getEstado());
        }
    }

    @Test
    @DisplayName("No debe cancelar citas pasadas de la serie")
    void noDebeCancelarCitasPasadas() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        List<Cita> citasFuturas = Arrays.asList(
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(1))
        );

        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(citasFuturas);
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int canceladas = citaRecurrenteService.cancelarSerieRecurrente(citaPadreId);

        // Assert
        assertEquals(1, canceladas);
        verify(citaRepository).findByCitaPadreIdAndFechaHoraAfter(eq(citaPadreId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Debe obtener todas las citas de una serie")
    void debeObtenerTodasCitasSerie() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        List<Cita> citasSerie = Arrays.asList(
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(1)),
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(2))
        );

        when(citaRepository.findByCitaPadreId(citaPadreId)).thenReturn(citasSerie);

        // Act
        List<Cita> resultado = citaRecurrenteService.obtenerSerieRecurrente(citaPadreId);

        // Assert
        assertEquals(2, resultado.size());
        verify(citaRepository).findByCitaPadreId(citaPadreId);
    }

    @Test
    @DisplayName("Debe actualizar notas en serie recurrente")
    void debeActualizarNotasSerieRecurrente() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        List<Cita> citasHijas = Arrays.asList(
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(1)),
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(2))
        );

        Cita cambios = Cita.builder()
                .notas("Notas actualizadas")
                .build();

        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(citasHijas);
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int actualizadas = citaRecurrenteService.actualizarSerieRecurrente(citaPadreId, cambios);

        // Assert
        assertEquals(2, actualizadas);
        for (Cita cita : citasHijas) {
            assertEquals("Notas actualizadas", cita.getNotas());
        }
        verify(citaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe actualizar precio en serie recurrente")
    void debeActualizarPrecioSerieRecurrente() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        List<Cita> citasHijas = Arrays.asList(
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(1))
        );

        Cita cambios = Cita.builder()
                .precio(new BigDecimal("200.00"))
                .build();

        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(citasHijas);
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int actualizadas = citaRecurrenteService.actualizarSerieRecurrente(citaPadreId, cambios);

        // Assert
        assertEquals(1, actualizadas);
        assertEquals(new BigDecimal("200.00"), citasHijas.get(0).getPrecio());
    }

    @Test
    @DisplayName("Debe actualizar estado en serie recurrente")
    void debeActualizarEstadoSerieRecurrente() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        List<Cita> citasHijas = Arrays.asList(
                crearCitaHija(citaPadreId, LocalDateTime.now().plusDays(1))
        );

        Cita cambios = Cita.builder()
                .estado(Cita.EstadoCita.CONFIRMADA)
                .build();

        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(citasHijas);
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int actualizadas = citaRecurrenteService.actualizarSerieRecurrente(citaPadreId, cambios);

        // Assert
        assertEquals(1, actualizadas);
        assertEquals(Cita.EstadoCita.CONFIRMADA, citasHijas.get(0).getEstado());
    }

    @Test
    @DisplayName("Debe manejar lista vacía al cancelar serie")
    void debeManejarListaVaciaAlCancelar() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int canceladas = citaRecurrenteService.cancelarSerieRecurrente(citaPadreId);

        // Assert
        assertEquals(0, canceladas);
        verify(citaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe manejar lista vacía al actualizar serie")
    void debeManejarListaVaciaAlActualizar() {
        // Arrange
        String citaPadreId = UUID.randomUUID().toString();
        Cita cambios = Cita.builder().notas("Nueva nota").build();

        when(citaRepository.findByCitaPadreIdAndFechaHoraAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(citaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int actualizadas = citaRecurrenteService.actualizarSerieRecurrente(citaPadreId, cambios);

        // Assert
        assertEquals(0, actualizadas);
        verify(citaRepository).saveAll(anyList());
    }

    // Métodos auxiliares

    private Cita crearCitaHija(String citaPadreId, LocalDateTime fechaHora) {
        return Cita.builder()
                .id(UUID.randomUUID().toString())
                .citaPadreId(citaPadreId)
                .fechaHora(fechaHora)
                .fechaFin(fechaHora.plusHours(1))
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .precio(new BigDecimal("150.00"))
                .esRecurrente(false)
                .tipoRecurrencia(TipoRecurrencia.NO_RECURRENTE)
                .build();
    }
}
