package com.reservas.repository;

import com.reservas.entity.CitaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitaServicioRepository extends JpaRepository<CitaServicio, String> {

    /**
     * Busca todos los servicios asociados a una cita
     */
    List<CitaServicio> findByCitaId(String citaId);

    /**
     * Elimina todos los servicios asociados a una cita
     */
    void deleteByCitaId(String citaId);
}
