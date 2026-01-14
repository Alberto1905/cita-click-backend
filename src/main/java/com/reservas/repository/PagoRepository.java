package com.reservas.repository;

import com.reservas.entity.Negocio;
import com.reservas.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    // Buscar por IDs de Stripe
    Optional<Pago> findByStripePaymentIntentId(String paymentIntentId);
    Optional<Pago> findByStripeCheckoutSessionId(String sessionId);

    // Buscar por negocio
    List<Pago> findByNegocioOrderByFechaCreacionDesc(Negocio negocio);
    List<Pago> findByNegocioAndEstadoOrderByFechaCreacionDesc(Negocio negocio, String estado);

    // Estadísticas
    @Query("SELECT COUNT(p) FROM Pago p WHERE p.negocio = :negocio AND p.estado = 'completed'")
    long countPagosCompletadosByNegocio(Negocio negocio);

    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.negocio = :negocio AND p.estado = 'completed'")
    java.math.BigDecimal sumMontoByNegocioAndEstadoCompleted(Negocio negocio);

    // Buscar pagos en un rango de fechas
    List<Pago> findByNegocioAndFechaCreacionBetween(Negocio negocio, LocalDateTime inicio, LocalDateTime fin);

    // Buscar último pago completado
    Optional<Pago> findFirstByNegocioAndEstadoOrderByFechaCompletadoDesc(Negocio negocio, String estado);

    // Buscar pagos pendientes
    @Query("SELECT p FROM Pago p WHERE p.estado = 'pending' AND p.fechaCreacion < :fechaLimite")
    List<Pago> findPagosPendientesAntesDe(LocalDateTime fechaLimite);
}
