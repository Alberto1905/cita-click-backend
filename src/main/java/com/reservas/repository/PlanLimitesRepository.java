package com.reservas.repository;

import com.reservas.entity.PlanLimites;
import com.reservas.entity.enums.TipoPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanLimitesRepository extends JpaRepository<PlanLimites, UUID> {

    Optional<PlanLimites> findByTipoPlan(TipoPlan tipoPlan);

    boolean existsByTipoPlan(TipoPlan tipoPlan);
}
