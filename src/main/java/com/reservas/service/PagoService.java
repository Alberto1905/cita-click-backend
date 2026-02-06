package com.reservas.service;

import com.reservas.dto.response.PagoResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.Pago;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.PagoRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar pagos de suscripciones
 *
 * @author Cita Click
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene el historial de pagos de un negocio
     */
    @Transactional(readOnly = true)
    public List<PagoResponse> obtenerHistorialPagos(String email) {
        log.info("[PagoService] Obteniendo historial de pagos para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado para el usuario");
        }

        List<Pago> pagos = pagoRepository.findByNegocioOrderByFechaCreacionDesc(negocio);

        log.info("[PagoService] Encontrados {} pagos para el negocio", pagos.size());

        return pagos.stream()
                .map(PagoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un pago específico por su ID
     */
    @Transactional(readOnly = true)
    public PagoResponse obtenerPago(String email, String pagoId) {
        log.info("[PagoService] Obteniendo pago {} para usuario: {}", pagoId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado para el usuario");
        }

        Pago pago = pagoRepository.findById(java.util.UUID.fromString(pagoId))
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));

        // Verificar que el pago pertenece al negocio del usuario
        if (!pago.getNegocio().getId().equals(negocio.getId())) {
            throw new UnauthorizedException("No tienes permiso para ver este pago");
        }

        return PagoResponse.fromEntity(pago);
    }

    /**
     * Obtiene estadísticas de pagos del negocio
     */
    @Transactional(readOnly = true)
    public EstadisticasPagosResponse obtenerEstadisticas(String email) {
        log.info("[PagoService] Obteniendo estadísticas de pagos para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado para el usuario");
        }

        long totalPagos = pagoRepository.countPagosCompletadosByNegocio(negocio);
        BigDecimal montoTotal = pagoRepository.sumMontoByNegocioAndEstadoCompleted(negocio);

        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }

        return EstadisticasPagosResponse.builder()
                .totalPagos(totalPagos)
                .montoTotal(montoTotal)
                .build();
    }

    /**
     * DTO para estadísticas de pagos
     */
    @lombok.Data
    @lombok.Builder
    public static class EstadisticasPagosResponse {
        private long totalPagos;
        private BigDecimal montoTotal;
    }
}
