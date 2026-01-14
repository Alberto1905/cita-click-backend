package com.reservas.repository;

import com.reservas.entity.HorarioTrabajo;
import com.reservas.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HorarioTrabajoRepository extends JpaRepository<HorarioTrabajo, UUID> {
    List<HorarioTrabajo> findByNegocioAndActivo(Negocio negocio, boolean activo);
    List<HorarioTrabajo> findByNegocio(Negocio negocio);
    List<HorarioTrabajo> findByNegocioAndDiaSemana(Negocio negocio, Integer diaSemana);
    void deleteByNegocioAndDiaSemana(Negocio negocio, Integer diaSemana);
}