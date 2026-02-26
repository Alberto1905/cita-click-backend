package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidad que representa el horario de trabajo de un negocio.
 *
 * Define los horarios de apertura y cierre para cada día de la semana.
 *
 * @author Cita Click
 */
@Data
@Entity
@Table(
    name = "tbl_horarios_trabajo",
    indexes = {
        @Index(name = "idx_horario_trabajo_negocio_id", columnList = "negocio_id"),
        @Index(name = "idx_horario_trabajo_dia_semana", columnList = "dia_semana"),
        @Index(name = "idx_horario_trabajo_negocio_activo", columnList = "negocio_id, activo")
    }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_horario_trabajo_negocio"))
    private Negocio negocio;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana; // 0-6 (Lunes-Domingo, 0=Lunes, 6=Domingo)

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean activo = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Verifica si el horario es válido (hora cierre > hora apertura).
     *
     * @return true si es válido
     */
    public boolean esHorarioValido() {
        return horaCierre.isAfter(horaApertura);
    }

    /**
     * Calcula la duración del horario en minutos.
     *
     * @return Minutos de trabajo
     */
    public long getDuracionMinutos() {
        return java.time.Duration.between(horaApertura, horaCierre).toMinutes();
    }
}
