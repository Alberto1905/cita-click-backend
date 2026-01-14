package com.reservas.repository;

import com.reservas.entity.Cita;
import com.reservas.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CitaRepository extends JpaRepository<Cita, String> {

    List<Cita> findByNegocio(Negocio negocio);

    List<Cita> findByNegocioAndEstado(Negocio negocio, Cita.EstadoCita estado);

    @Query("SELECT c FROM Cita c WHERE c.negocio = :negocio AND " +
           "CAST(c.fechaHora AS LocalDate) = :fecha")
    List<Cita> findByNegocioAndFecha(@Param("negocio") Negocio negocio, @Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Cita c WHERE c.negocio = :negocio AND " +
           "CAST(c.fechaHora AS LocalDate) = :fecha AND c.estado = :estado")
    List<Cita> findByNegocioAndFechaAndEstado(
        @Param("negocio") Negocio negocio,
        @Param("fecha") LocalDate fecha,
        @Param("estado") Cita.EstadoCita estado
    );

    @Query("SELECT c FROM Cita c WHERE c.negocio = :negocio AND " +
           "c.estado NOT IN ('CANCELADA', 'COMPLETADA') AND " +
           "((c.fechaHora < :fechaFin AND c.fechaFin > :fechaHora))")
    List<Cita> findConflictingCitas(
        @Param("negocio") Negocio negocio,
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT c FROM Cita c WHERE c.negocio = :negocio AND " +
           "c.fechaHora BETWEEN :fechaInicio AND :fechaFin")
    List<Cita> findByNegocioAndFechaHoraBetween(
        @Param("negocio") Negocio negocio,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT c FROM Cita c WHERE c.negocio = :negocio AND " +
           "c.estado IN ('CONFIRMADA', 'COMPLETADA') AND " +
           "c.fechaHora BETWEEN :fechaInicio AND :fechaFin")
    List<Cita> findCitasCompletadasYConfirmadas(
        @Param("negocio") Negocio negocio,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT c FROM Cita c WHERE c.negocio = :negocio AND " +
           "c.fechaHora BETWEEN :fechaInicio AND :fechaFin AND c.estado = :estado")
    List<Cita> findByNegocioAndFechaHoraBetweenAndEstado(
        @Param("negocio") Negocio negocio,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        @Param("estado") Cita.EstadoCita estado
    );

    List<Cita> findByNegocioOrderByFechaHoraAsc(Negocio negocio);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.negocio.id = :negocioId AND " +
           "YEAR(c.fechaHora) = :year AND MONTH(c.fechaHora) = :month")
    long countCitasByNegocioAndMonth(
        @Param("negocioId") UUID negocioId,
        @Param("year") int year,
        @Param("month") int month
    );

    // Métodos para citas recurrentes
    List<Cita> findByCitaPadreId(String citaPadreId);

    @Query("SELECT c FROM Cita c WHERE c.citaPadreId = :citaPadreId AND c.fechaHora > :fecha")
    List<Cita> findByCitaPadreIdAndFechaHoraAfter(
        @Param("citaPadreId") String citaPadreId,
        @Param("fecha") LocalDateTime fecha
    );
}
