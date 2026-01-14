package com.reservas.repository;

import com.reservas.entity.Payment;
import com.reservas.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    Page<Payment> findByUsuarioId(UUID usuarioId, Pageable pageable);

    List<Payment> findByUsuarioIdAndStatus(UUID usuarioId, Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.usuario.id = ?1 AND p.createdAt BETWEEN ?2 AND ?3")
    List<Payment> findByUsuarioIdAndDateRange(UUID usuarioId, LocalDateTime start, LocalDateTime end);

    List<Payment> findByCitaId(String citaId);

    Long countByUsuarioId(UUID usuarioId);

    @Query("SELECT SUM(p.netAmount) FROM Payment p WHERE p.usuario.id = ?1 AND p.status = 'SUCCESS'")
    java.math.BigDecimal getTotalNetAmountByUsuarioId(UUID usuarioId);
}
