package com.reservas.repository;

import com.reservas.entity.Negocio;
import com.reservas.entity.UsoNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsoNegocioRepository extends JpaRepository<UsoNegocio, UUID> {

    Optional<UsoNegocio> findByNegocioAndPeriodo(Negocio negocio, String periodo);

    @Query("SELECT u FROM UsoNegocio u WHERE u.negocio.id = :negocioId AND u.periodo = :periodo")
    Optional<UsoNegocio> findByNegocioIdAndPeriodo(@Param("negocioId") UUID negocioId, @Param("periodo") String periodo);

    boolean existsByNegocioAndPeriodo(Negocio negocio, String periodo);
}
