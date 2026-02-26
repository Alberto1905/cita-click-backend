package com.reservas.entity;

import com.reservas.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que representa un servicio ofrecido por un negocio.
 *
 * Cada servicio tiene nombre, precio, duración y puede estar activo o inactivo.
 *
 * @author Cita Click
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "tbl_servicios",
    indexes = {
        @Index(name = "idx_servicio_negocio_id", columnList = "negocio_id"),
        @Index(name = "idx_servicio_activo", columnList = "activo"),
        @Index(name = "idx_servicio_negocio_activo", columnList = "negocio_id, activo")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_servicio_negocio"))
    private Negocio negocio;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "duracion_minutos", nullable = false)
    @Builder.Default
    private Integer duracionMinutos = 30;

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean activo = true;

    /**
     * Verifica si el servicio está activo y disponible.
     *
     * @return true si está activo
     */
    public boolean estaDisponible() {
        return activo;
    }

    /**
     * Calcula el precio por minuto del servicio.
     *
     * @return Precio por minuto
     */
    public BigDecimal getPrecioPorMinuto() {
        if (duracionMinutos == null || duracionMinutos == 0) {
            return BigDecimal.ZERO;
        }
        return precio.divide(BigDecimal.valueOf(duracionMinutos), 2, BigDecimal.ROUND_HALF_UP);
    }
}
