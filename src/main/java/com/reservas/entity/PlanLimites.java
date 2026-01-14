package com.reservas.entity;

import com.reservas.entity.enums.TipoPlan;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que define los límites de cada plan
 */
@Data
@Entity
@Table(name = "tbl_plan_limites", schema = "ccdiad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanLimites {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private TipoPlan tipoPlan;

    @Column(nullable = false)
    private Integer maxUsuarios;

    @Column(nullable = false)
    private Integer maxClientes;

    @Column(nullable = false)
    private Integer maxCitasMes;

    @Column(nullable = false)
    private Integer maxServicios;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean smsWhatsappHabilitado = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean reportesAvanzadosHabilitado = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean soportePrioritario = false;

    // Método helper para verificar si es plan ilimitado
    public boolean esIlimitado() {
        return tipoPlan == TipoPlan.PREMIUM;
    }
}
