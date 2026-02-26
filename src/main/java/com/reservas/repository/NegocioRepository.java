package com.reservas.repository;

import com.reservas.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NegocioRepository extends JpaRepository<Negocio, UUID> {
    Optional<Negocio> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Negocio> findByStripeSubscriptionId(String stripeSubscriptionId);
}