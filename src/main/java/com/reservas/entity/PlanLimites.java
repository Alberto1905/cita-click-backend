package com.reservas.entity;

import com.reservas.entity.enums.TipoPlan;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que define los límites de cada plan de suscripción.
 *
 * Define las restricciones y características de cada tipo de plan:
 * - BASICO: Límites básicos para negocios pequeños
 * - PROFESIONAL: Límites ampliados para negocios medianos
 * - PREMIUM: Sin límites para negocios grandes
 *
 * @author Cita Click
 */
@Data
@Entity
@Table(
    name = "tbl_plan_limites",
    indexes = {
        @Index(name = "idx_plan_limites_tipo_plan", columnList = "tipo_plan")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_limites_tipo_plan", columnNames = "tipo_plan")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanLimites {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_plan", nullable = false, unique = true, length = 50)
    private TipoPlan tipoPlan;

    @Column(name = "max_usuarios", nullable = false)
    private Integer maxUsuarios;

    @Column(name = "max_clientes", nullable = false)
    private Integer maxClientes;

    @Column(name = "max_citas_mes", nullable = false)
    private Integer maxCitasMes;

    @Column(name = "max_servicios", nullable = false)
    private Integer maxServicios;

    @Column(name = "email_recordatorios_habilitado", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean emailRecordatoriosHabilitado = false;

    @Column(name = "sms_whatsapp_habilitado", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean smsWhatsappHabilitado = false;

    @Column(name = "reportes_avanzados_habilitado", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean reportesAvanzadosHabilitado = false;

    @Column(name = "personalizacion_email_habilitado", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean personalizacionEmailHabilitado = false;

    @Column(name = "soporte_prioritario", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean soportePrioritario = false;

    /**
     * Método helper para verificar si es plan ilimitado (PREMIUM).
     *
     * @return true si el plan es PREMIUM
     */
    public boolean esIlimitado() {
        return tipoPlan == TipoPlan.PREMIUM;
    }

    /**
     * Verifica si un valor supera el límite establecido.
     * Si es ilimitado, siempre retorna false.
     *
     * @param valor Valor actual a verificar
     * @param limite Límite del plan
     * @return true si supera el límite
     */
    private boolean superaLimite(int valor, int limite) {
        if (esIlimitado()) {
            return false;
        }
        return valor > limite;
    }

    /**
     * Verifica si se superó el límite de usuarios.
     */
    public boolean superaLimiteUsuarios(int cantidadActual) {
        return superaLimite(cantidadActual, maxUsuarios);
    }

    /**
     * Verifica si se superó el límite de clientes.
     */
    public boolean superaLimiteClientes(int cantidadActual) {
        return superaLimite(cantidadActual, maxClientes);
    }

    /**
     * Verifica si se superó el límite de citas mensuales.
     */
    public boolean superaLimiteCitasMes(int cantidadActual) {
        return superaLimite(cantidadActual, maxCitasMes);
    }

    /**
     * Verifica si se superó el límite de servicios.
     */
    public boolean superaLimiteServicios(int cantidadActual) {
        return superaLimite(cantidadActual, maxServicios);
    }
}
