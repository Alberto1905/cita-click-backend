package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una suscripción al SaaS (Stripe Billing).
 *
 * Esta entidad registra las suscripciones de los USUARIOS DEL SAAS,
 * NO de los clientes finales.
 *
 * SEPARACIÓN IMPORTANTE:
 * - StripeSubscription: Usuario paga plan mensual/anual del SaaS → va a cuenta principal
 * - Payment: Cliente final paga a usuario del SaaS → va a cuenta conectada
 *
 * FLUJO:
 * 1. Usuario se registra en el SaaS
 * 2. Selecciona un plan (Free, Pro, Enterprise)
 * 3. Se crea Customer en Stripe
 * 4. Se crea Subscription
 * 5. Stripe factura automáticamente cada mes/año
 * 6. Webhooks actualizan el estado
 */
@Entity
@Table(name = "tbl_stripe_subscriptions", schema = "ccdiad", indexes = {
    @Index(name = "idx_stripe_subscriptions_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_stripe_subscriptions_usuario_id", columnList = "usuario_id"),
    @Index(name = "idx_stripe_subscriptions_status", columnList = "status"),
    @Index(name = "idx_stripe_subscriptions_period_end", columnList = "current_period_end")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Usuario del SaaS que tiene la suscripción.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * ID del Customer en Stripe (cus_xxxxx).
     * Representa al usuario en Stripe Billing.
     */
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    /**
     * ID de la Subscription en Stripe (sub_xxxxx).
     */
    @Column(name = "subscription_id", unique = true, length = 100)
    private String subscriptionId;

    /**
     * ID del Plan/Price en Stripe (price_xxxxx).
     * Define qué plan tiene contratado (Basic, Pro, Enterprise).
     */
    @Column(name = "price_id", nullable = false, length = 100)
    private String priceId;

    /**
     * Nombre del plan (para mostrar en la UI).
     */
    @Column(name = "plan_name", length = 100)
    private String planName;

    /**
     * Estado de la suscripción.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    /**
     * Monto del plan (precio por periodo).
     */
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Moneda del plan.
     */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * Intervalo de facturación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", length = 10)
    private BillingInterval billingInterval;

    /**
     * Inicio del periodo actual.
     */
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    /**
     * Fin del periodo actual.
     * Importante para calcular acceso y renovación.
     */
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    /**
     * Indica si la suscripción se cancelará al final del periodo.
     */
    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd;

    /**
     * Fecha en que se canceló (si aplica).
     */
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    /**
     * Fecha en que terminó/expira la suscripción.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * ID del método de pago por defecto.
     */
    @Column(name = "default_payment_method_id", length = 100)
    private String defaultPaymentMethodId;

    /**
     * Últimos 4 dígitos de la tarjeta.
     */
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    /**
     * Marca de la tarjeta.
     */
    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    /**
     * Indica si está en periodo de prueba.
     */
    @Column(name = "trial_period", nullable = false)
    private Boolean trialPeriod;

    /**
     * Fecha de inicio del trial.
     */
    @Column(name = "trial_start")
    private LocalDateTime trialStart;

    /**
     * Fecha de fin del trial.
     */
    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    /**
     * Última factura generada (invoice_id).
     */
    @Column(name = "latest_invoice_id", length = 100)
    private String latestInvoiceId;

    /**
     * Metadatos adicionales.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Estados de suscripción
     */
    public enum SubscriptionStatus {
        /**
         * Suscripción activa y pagada
         */
        ACTIVE,

        /**
         * En periodo de prueba
         */
        TRIALING,

        /**
         * Incompleta (esperando pago)
         */
        INCOMPLETE,

        /**
         * Primer pago expiró
         */
        INCOMPLETE_EXPIRED,

        /**
         * Impagada (past due)
         */
        PAST_DUE,

        /**
         * Cancelada
         */
        CANCELED,

        /**
         * No renovada/expirada
         */
        UNPAID
    }

    /**
     * Intervalo de facturación
     */
    public enum BillingInterval {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    /**
     * Helper para verificar si la suscripción está activa.
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
                || status == SubscriptionStatus.TRIALING;
    }

    /**
     * Helper para verificar si tiene acceso al SaaS.
     */
    public boolean hasAccess() {
        if (!isActive()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return currentPeriodEnd == null || now.isBefore(currentPeriodEnd);
    }
}
