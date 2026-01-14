package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tbl_negocios", schema = "ccdiad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(unique = true, nullable = false)
    private String email;

    private String telefono;

    private String tipo; // 'salon', 'clinica', 'masajes', etc.

    private String logoUrl;

    // Legacy flat address fields (kept for backward compatibility)
    private String domicilio;

    private String ciudad;

    private String pais;

    // Individual address fields for structured storage
    @Column(name = "direccion_calle")
    private String direccionCalle;

    @Column(name = "direccion_colonia")
    private String direccionColonia;

    @Column(name = "direccion_codigo_postal", length = 10)
    private String direccionCodigoPostal;

    @Column(name = "direccion_estado", length = 100)
    private String direccionEstado;

    @Column(columnDefinition = "varchar(50) default 'trial'")
    @Builder.Default
    private String estadoPago = "trial"; // 'trial', 'activo', 'vencido', 'suspendido'

    @Column(columnDefinition = "varchar(50) default 'basico'")
    @Builder.Default
    private String plan = "basico"; // 'basico', 'profesional', 'premium'

    private LocalDateTime fechaInicioPlan;

    private LocalDateTime fechaProximoCobro;

    // Stripe Customer ID
    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    // Nuevos campos para control de suscripción
    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaFinPrueba;

    @Column(nullable = false)
    @Builder.Default
    private boolean enPeriodoPrueba = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean cuentaActiva = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificacionPruebaEnviada = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificacionVencimientoEnviada = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        fechaRegistro = LocalDateTime.now();
        fechaInicioPlan = LocalDateTime.now();

        // Solo planes Básico y Profesional tienen prueba
        if ("basico".equalsIgnoreCase(plan) || "profesional".equalsIgnoreCase(plan)) {
            fechaFinPrueba = LocalDateTime.now().plusDays(7);
            enPeriodoPrueba = true;
            estadoPago = "trial";
        } else {
            // Premium no tiene prueba, requiere pago inmediato
            enPeriodoPrueba = false;
            estadoPago = "pendiente_pago";
            cuentaActiva = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Métodos de utilidad
    public boolean haVencidoPrueba() {
        if (!enPeriodoPrueba || fechaFinPrueba == null) return false;
        return LocalDateTime.now().isAfter(fechaFinPrueba);
    }

    public boolean puedeUsarSistema() {
        // Si está en periodo de prueba y no ha vencido
        if (enPeriodoPrueba && fechaFinPrueba != null) {
            return !haVencidoPrueba() && cuentaActiva;
        }
        // Si tiene suscripción activa
        return "activo".equals(estadoPago) && cuentaActiva;
    }

    public long diasRestantesPrueba() {
        if (!enPeriodoPrueba || fechaFinPrueba == null) return 0;

        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isAfter(fechaFinPrueba)) return 0;

        return java.time.Duration.between(ahora, fechaFinPrueba).toDays();
    }

    public long diasRestantesVencimiento() {
        if (fechaProximoCobro == null) return 0;

        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isAfter(fechaProximoCobro)) return 0;

        return java.time.Duration.between(ahora, fechaProximoCobro).toDays();
    }

    public boolean necesitaNotificacionPrueba() {
        // Notificar 1 día antes de que termine la prueba
        return enPeriodoPrueba && !notificacionPruebaEnviada &&
               diasRestantesPrueba() <= 1 && diasRestantesPrueba() > 0;
    }

    public boolean necesitaNotificacionVencimiento() {
        // Notificar 5 días antes del vencimiento
        return !enPeriodoPrueba && "activo".equals(estadoPago) &&
               !notificacionVencimientoEnviada &&
               diasRestantesVencimiento() <= 5 && diasRestantesVencimiento() > 0;
    }
}