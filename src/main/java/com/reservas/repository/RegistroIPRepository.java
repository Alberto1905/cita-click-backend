package com.reservas.repository;

import com.reservas.entity.RegistroIP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RegistroIPRepository extends JpaRepository<RegistroIP, UUID> {

    /**
     * Busca registros de prueba activos desde una IP en los últimos N días
     */
    @Query("SELECT r FROM RegistroIP r WHERE r.ipAddress = ?1 " +
           "AND r.esPrueba = true " +
           "AND r.activo = true " +
           "AND r.createdAt >= ?2")
    List<RegistroIP> findActivosRecentesByIP(String ipAddress, LocalDateTime desde);

    /**
     * Cuenta registros de prueba desde una IP en un periodo
     */
    @Query("SELECT COUNT(r) FROM RegistroIP r WHERE r.ipAddress = ?1 " +
           "AND r.esPrueba = true " +
           "AND r.createdAt >= ?2")
    long countRegistrosPruebaByIP(String ipAddress, LocalDateTime desde);

    /**
     * Busca todos los registros de una IP
     */
    List<RegistroIP> findByIpAddress(String ipAddress);
}
