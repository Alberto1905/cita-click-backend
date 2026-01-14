package com.reservas.repository;

import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, String> {

    List<Servicio> findByNegocio(Negocio negocio);

    List<Servicio> findByNegocioAndActivoTrue(Negocio negocio);

    @Query("SELECT COUNT(s) FROM Servicio s WHERE s.negocio.id = :negocioId")
    long countByNegocioId(@Param("negocioId") UUID negocioId);
}
