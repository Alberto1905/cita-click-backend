package com.reservas.service;

import com.reservas.dto.response.PagoResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.Pago;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.PagoRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService - Pruebas Unitarias")
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PagoService pagoService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private Pago pagoMock1;
    private Pago pagoMock2;

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

        pagoMock1 = Pago.builder()
                .id(UUID.randomUUID())
                .negocio(negocioMock)
                .monto(new BigDecimal("299.00"))
                .moneda("MXN")
                .estado("completado")
                .metodoPago("stripe")
                .plan("profesional")
                .fechaCreacion(LocalDateTime.now().minusDays(30))
                .build();

        pagoMock2 = Pago.builder()
                .id(UUID.randomUUID())
                .negocio(negocioMock)
                .monto(new BigDecimal("299.00"))
                .moneda("MXN")
                .estado("completado")
                .metodoPago("stripe")
                .plan("profesional")
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe obtener historial de pagos exitosamente")
    void debeObtenerHistorialPagos_exitosamente() {
        // Arrange
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.findByNegocioOrderByFechaCreacionDesc(negocioMock))
                .thenReturn(Arrays.asList(pagoMock2, pagoMock1));

        // Act
        List<PagoResponse> resultado = pagoService.obtenerHistorialPagos("usuario@test.com");

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(usuarioRepository, times(1)).findByEmail("usuario@test.com");
        verify(pagoRepository, times(1)).findByNegocioOrderByFechaCreacionDesc(negocioMock);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener historial cuando usuario no existe")
    void debeLanzarExcepcion_alObtenerHistorial_cuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail("noexiste@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> pagoService.obtenerHistorialPagos("noexiste@test.com"));

        verify(pagoRepository, never()).findByNegocioOrderByFechaCreacionDesc(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener historial cuando negocio no existe")
    void debeLanzarExcepcion_alObtenerHistorial_cuandoNegocioNoExiste() {
        // Arrange
        usuarioMock.setNegocio(null);
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> pagoService.obtenerHistorialPagos("usuario@test.com"));

        verify(pagoRepository, never()).findByNegocioOrderByFechaCreacionDesc(any());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay pagos")
    void debeRetornarListaVacia_cuandoNoHayPagos() {
        // Arrange
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.findByNegocioOrderByFechaCreacionDesc(negocioMock))
                .thenReturn(Collections.emptyList());

        // Act
        List<PagoResponse> resultado = pagoService.obtenerHistorialPagos("usuario@test.com");

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Debe obtener pago específico exitosamente")
    void debeObtenerPagoEspecifico_exitosamente() {
        // Arrange
        String pagoId = pagoMock1.getId().toString();
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.findById(pagoMock1.getId()))
                .thenReturn(Optional.of(pagoMock1));

        // Act
        PagoResponse resultado = pagoService.obtenerPago("usuario@test.com", pagoId);

        // Assert
        assertNotNull(resultado);
        assertEquals(pagoMock1.getId().toString(), resultado.getId());
        verify(pagoRepository, times(1)).findById(pagoMock1.getId());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando pago no existe")
    void debeLanzarExcepcion_cuandoPagoNoExiste() {
        // Arrange
        UUID pagoIdInexistente = UUID.randomUUID();
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.findById(pagoIdInexistente))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> pagoService.obtenerPago("usuario@test.com", pagoIdInexistente.toString()));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando pago no pertenece al usuario")
    void debeLanzarExcepcion_cuandoPagoNoPerteneceusuario() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();

        pagoMock1.setNegocio(otroNegocio);

        String pagoId = pagoMock1.getId().toString();
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.findById(pagoMock1.getId()))
                .thenReturn(Optional.of(pagoMock1));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
                () -> pagoService.obtenerPago("usuario@test.com", pagoId));
    }

    @Test
    @DisplayName("Debe obtener estadísticas de pagos correctamente")
    void debeObtenerEstadisticas_correctamente() {
        // Arrange
        BigDecimal montoTotal = new BigDecimal("598.00");
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.countPagosCompletadosByNegocio(negocioMock))
                .thenReturn(2L);
        when(pagoRepository.sumMontoByNegocioAndEstadoCompleted(negocioMock))
                .thenReturn(montoTotal);

        // Act
        PagoService.EstadisticasPagosResponse estadisticas =
                pagoService.obtenerEstadisticas("usuario@test.com");

        // Assert
        assertNotNull(estadisticas);
        assertEquals(2L, estadisticas.getTotalPagos());
        assertEquals(montoTotal, estadisticas.getMontoTotal());
    }

    @Test
    @DisplayName("Debe retornar cero cuando no hay pagos completados")
    void debeRetornarCero_cuandoNoHayPagosCompletados() {
        // Arrange
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(pagoRepository.countPagosCompletadosByNegocio(negocioMock))
                .thenReturn(0L);
        when(pagoRepository.sumMontoByNegocioAndEstadoCompleted(negocioMock))
                .thenReturn(null);

        // Act
        PagoService.EstadisticasPagosResponse estadisticas =
                pagoService.obtenerEstadisticas("usuario@test.com");

        // Assert
        assertNotNull(estadisticas);
        assertEquals(0L, estadisticas.getTotalPagos());
        assertEquals(BigDecimal.ZERO, estadisticas.getMontoTotal());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener estadísticas cuando usuario no existe")
    void debeLanzarExcepcion_alObtenerEstadisticas_cuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByEmail("noexiste@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> pagoService.obtenerEstadisticas("noexiste@test.com"));

        verify(pagoRepository, never()).countPagosCompletadosByNegocio(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener estadísticas cuando negocio no existe")
    void debeLanzarExcepcion_alObtenerEstadisticas_cuandoNegocioNoExiste() {
        // Arrange
        usuarioMock.setNegocio(null);
        when(usuarioRepository.findByEmail("usuario@test.com"))
                .thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> pagoService.obtenerEstadisticas("usuario@test.com"));

        verify(pagoRepository, never()).countPagosCompletadosByNegocio(any());
    }
}
