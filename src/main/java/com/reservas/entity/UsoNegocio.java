package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Entidad que trackea el uso mensual de cada negocio
 */
@Data
@Entity
@Table(name = "tbl_uso_negocio",
       uniqueConstraints = @UniqueConstraint(columnNames = {"negocio_id", "periodo"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsoNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false)
    private String periodo; // Formato: "YYYY-MM" (ej: "2025-01")

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalUsuarios = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalClientes = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalCitasMes = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalServicios = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method para obtener el periodo actual
     */
    public static String getPeriodoActual() {
        YearMonth actual = YearMonth.now();
        return actual.toString(); // "YYYY-MM"
    }

    /**
     * Helper method para verificar si es el periodo actual
     */
    public boolean esPeriodoActual() {
        return this.periodo.equals(getPeriodoActual());
    }
}
