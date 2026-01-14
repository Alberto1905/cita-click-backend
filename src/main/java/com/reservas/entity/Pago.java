package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para registrar pagos de suscripciones procesados por Stripe
 */
@Data
@Entity
@Table(name = "tbl_pagos", schema = "ccdiad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    // IDs de Stripe
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(name = "stripe_checkout_session_id", unique = true)
    private String stripeCheckoutSessionId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    // Detalles del pago
    @Column(nullable = false)
    private String plan; // starter, professional, enterprise

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String moneda; // MXN, USD

    @Column(nullable = false)
    private String estado; // pending, completed, failed, refunded

    @Column(name = "metodo_pago")
    private String metodoPago; // card, oxxo, spei

    // Información adicional
    @Column(name = "periodo_inicio")
    private LocalDateTime periodoInicio;

    @Column(name = "periodo_fin")
    private LocalDateTime periodoFin;

    @Column(name = "email_cliente")
    private String emailCliente;

    private String descripcion;

    @Column(name = "factura_url")
    private String facturaUrl; // URL de la factura en Stripe

    // Metadata
    @Column(name = "stripe_metadata", columnDefinition = "TEXT")
    private String stripeMetadata; // JSON con metadata adicional de Stripe

    @Column(name = "error_mensaje")
    private String errorMensaje; // Si el pago falló

    // Auditoría
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // Métodos de utilidad
    public boolean isPagado() {
        return "completed".equals(estado);
    }

    public boolean isPendiente() {
        return "pending".equals(estado);
    }

    public boolean isFallido() {
        return "failed".equals(estado);
    }
}
