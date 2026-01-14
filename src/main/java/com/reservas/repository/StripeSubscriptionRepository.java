package com.reservas.repository;

import com.reservas.entity.StripeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, String> {

    Optional<StripeSubscription> findBySubscriptionId(String subscriptionId);

    Optional<StripeSubscription> findByCustomerId(String customerId);

    List<StripeSubscription> findByUsuarioId(UUID usuarioId);

    Optional<StripeSubscription> findByUsuarioIdAndStatus(UUID usuarioId, StripeSubscription.SubscriptionStatus status);

    List<StripeSubscription> findByStatus(StripeSubscription.SubscriptionStatus status);

    List<StripeSubscription> findByCurrentPeriodEndBefore(LocalDateTime date);

    boolean existsByUsuarioIdAndStatus(UUID usuarioId, StripeSubscription.SubscriptionStatus status);
}
