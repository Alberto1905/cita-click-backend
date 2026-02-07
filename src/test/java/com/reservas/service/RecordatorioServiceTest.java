package com.reservas.service;

import com.reservas.entity.*;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.RecordatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordatorioService - Pruebas Unitarias")
class RecordatorioServiceTest {

    @Mock
    private RecordatorioRepository recordatorioRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private SmsService smsService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RecordatorioService recordatorioService;

    private Negocio negocioMock;
    private Cliente clienteMock;
    private Servicio servicioMock;
    private Usuario usuarioMock;
    private Cita citaMock;

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

        citaMock = Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(LocalDateTime.now().plusDays(2))
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .build();
    }

    @Test
    @DisplayName("Debe crear recordatorio EMAIL cuando cliente tiene email")
    void debeCrearRecordatorioEmail_cuandoClienteTieneEmail() {
        // Arrange
        when(recordatorioRepository.save(any(Recordatorio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        recordatorioService.crearRecordatoriosParaCita(citaMock);

        // Assert
        verify(recordatorioRepository, times(1)).save(argThat(recordatorio ->
                recordatorio.getTipo() == Recordatorio.TipoRecordatorio.EMAIL &&
                !recordatorio.isEnviado() &&
                recordatorio.getCita().equals(citaMock)
        ));
    }

    @Test
    @DisplayName("No debe crear recordatorio SMS (está deshabilitado)")
    void noDebeCrearRecordatorioSMS() {
        // Arrange
        when(recordatorioRepository.save(any(Recordatorio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        recordatorioService.crearRecordatoriosParaCita(citaMock);

        // Assert - Solo debe crear EMAIL, NO SMS
        verify(recordatorioRepository, times(1)).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("No debe crear recordatorios si cliente no tiene email")
    void noDebeCrearRecordatorios_siClienteNoTieneEmail() {
        // Arrange
        clienteMock.setEmail(null);

        // Act
        recordatorioService.crearRecordatoriosParaCita(citaMock);

        // Assert
        verify(recordatorioRepository, never()).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("No debe crear recordatorios si email está vacío")
    void noDebeCrearRecordatorios_siEmailVacio() {
        // Arrange
        clienteMock.setEmail("   ");

        // Act
        recordatorioService.crearRecordatoriosParaCita(citaMock);

        // Assert
        verify(recordatorioRepository, never()).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Debe procesar recordatorios pendientes correctamente")
    void debeProcesarRecordatoriosPendientes() {
        // Arrange
        Recordatorio recordatorioEmail = Recordatorio.builder()
                .id("rec-1")
                .cita(citaMock)
                .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                .enviado(false)
                .build();

        // Configurar cita para que sea momento de enviar (1 hora en el futuro)
        citaMock.setFechaHora(LocalDateTime.now().plusHours(1));

        when(recordatorioRepository.findByEnviadoFalse())
                .thenReturn(Arrays.asList(recordatorioEmail));
        when(emailService.enviarRecordatorioCita(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(true);
        when(recordatorioRepository.save(any(Recordatorio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        recordatorioService.procesarRecordatoriosPendientes();

        // Assert
        verify(recordatorioRepository).findByEnviadoFalse();
        verify(emailService).enviarRecordatorioCita(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
        verify(recordatorioRepository).save(argThat(r -> r.isEnviado()));
    }

    @Test
    @DisplayName("No debe enviar recordatorios si aún no es el momento")
    void noDebeEnviarRecordatorios_siAunNoEsMomento() {
        // Arrange
        Recordatorio recordatorioEmail = Recordatorio.builder()
                .id("rec-1")
                .cita(citaMock)
                .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                .enviado(false)
                .build();

        // Cita muy en el futuro (más de 24 horas)
        citaMock.setFechaHora(LocalDateTime.now().plusDays(5));

        when(recordatorioRepository.findByEnviadoFalse())
                .thenReturn(Arrays.asList(recordatorioEmail));

        // Act
        recordatorioService.procesarRecordatoriosPendientes();

        // Assert
        verify(emailService, never()).enviarRecordatorioCita(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debe manejar errores al enviar sin lanzar excepciones")
    void debeManejarErroresAlEnviar() {
        // Arrange
        Recordatorio recordatorioEmail = Recordatorio.builder()
                .id("rec-1")
                .cita(citaMock)
                .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                .enviado(false)
                .build();

        citaMock.setFechaHora(LocalDateTime.now().plusHours(1));

        when(recordatorioRepository.findByEnviadoFalse())
                .thenReturn(Arrays.asList(recordatorioEmail));
        when(emailService.enviarRecordatorioCita(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error de red"));

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> recordatorioService.procesarRecordatoriosPendientes());
    }

    @Test
    @DisplayName("No debe marcar como enviado si el envío falla")
    void noDebMarcarComoEnviado_siFallaEnvio() {
        // Arrange
        Recordatorio recordatorioEmail = Recordatorio.builder()
                .id("rec-1")
                .cita(citaMock)
                .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                .enviado(false)
                .build();

        citaMock.setFechaHora(LocalDateTime.now().plusHours(1));

        when(recordatorioRepository.findByEnviadoFalse())
                .thenReturn(Arrays.asList(recordatorioEmail));
        when(emailService.enviarRecordatorioCita(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        // Act
        recordatorioService.procesarRecordatoriosPendientes();

        // Assert - NO debe guardar si falló
        verify(recordatorioRepository, never()).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Debe procesar múltiples recordatorios correctamente")
    void debeProcesarMultiplesRecordatorios() {
        // Arrange
        Cita cita2 = Cita.builder()
                .id(UUID.randomUUID().toString())
                .fechaHora(LocalDateTime.now().plusHours(2))
                .estado(Cita.EstadoCita.PENDIENTE)
                .cliente(clienteMock)
                .servicio(servicioMock)
                .negocio(negocioMock)
                .usuario(usuarioMock)
                .build();

        Recordatorio rec1 = Recordatorio.builder()
                .id("rec-1")
                .cita(citaMock)
                .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                .enviado(false)
                .build();

        Recordatorio rec2 = Recordatorio.builder()
                .id("rec-2")
                .cita(cita2)
                .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                .enviado(false)
                .build();

        citaMock.setFechaHora(LocalDateTime.now().plusHours(1));
        cita2.setFechaHora(LocalDateTime.now().plusHours(2));

        when(recordatorioRepository.findByEnviadoFalse())
                .thenReturn(Arrays.asList(rec1, rec2));
        when(emailService.enviarRecordatorioCita(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(recordatorioRepository.save(any(Recordatorio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        recordatorioService.procesarRecordatoriosPendientes();

        // Assert
        verify(emailService, times(2)).enviarRecordatorioCita(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(recordatorioRepository, times(2)).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Debe manejar lista vacía de recordatorios pendientes")
    void debeManejarListaVacia() {
        // Arrange
        when(recordatorioRepository.findByEnviadoFalse())
                .thenReturn(new ArrayList<>());

        // Act & Assert
        assertDoesNotThrow(() -> recordatorioService.procesarRecordatoriosPendientes());
        verify(emailService, never()).enviarRecordatorioCita(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
