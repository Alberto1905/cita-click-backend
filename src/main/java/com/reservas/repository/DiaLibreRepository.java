package com.reservas.repository;

import com.reservas.entity.DiaLibre;
import com.reservas.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiaLibreRepository extends JpaRepository<DiaLibre, String> {
    List<DiaLibre> findByNegocio(Negocio negocio);

    List<DiaLibre> findByNegocioId(UUID negocioId);

    Optional<DiaLibre> findByNegocioIdAndFecha(UUID negocioId, LocalDate fecha);

    List<DiaLibre> findByNegocioAndFecha(Negocio negocio, LocalDate fecha);
}