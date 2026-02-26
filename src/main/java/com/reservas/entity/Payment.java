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
 * Entidad que representa un pago procesado a través de Stripe Connect.
 *
 * Esta entidad registra TODOS los pagos que los clientes finales hacen
 * a los usuarios del SaaS.
 *
 * FLUJO:
 * 1. Cliente final inicia pago
 * 2. Se crea PaymentIntent en Stripe
 * 3. Se guarda registro en esta tabla con estado PENDING
 * 4. Webhook confirma pago → estado SUCCESS
 * 5. Dinero va a cuenta conectada del usuario (menos comisión plataforma)
 */
@Entity
@Table(name = "tbl_payments", indexes = {
    @Index(name = "idx_payment_intent_id", columnList = "payment_intent_id"),
    @Index(name = "idx_payments_usuario_id", columnList = "usuario_id"),
    @Index(name = "idx_payments_cita_id", columnList = "cita_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Usuario del SaaS que recibe el pago.
     * Propietario de la cuenta conectada.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Cuenta conectada de Stripe del usuario.
     */
    @ManyToOne
    @JoinColumn(name = "stripe_account_id")
    private StripeConnectedAccount stripeAccount;

    /**
     * Cita asociada al pago (si aplica).
     * Puede ser null si el pago no está relacionado con una cita.
     */
    @ManyToOne
    @JoinColumn(name = "cita_id")
    private Cita cita;

    /**
     * ID del PaymentIntent en Stripe (pi_xxxxx).
     * Este es el identificador único del pago en Stripe.
     */
    @Column(name = "payment_intent_id", nullable = false, unique = true, length = 100)
    private String paymentIntentId;

    /**
     * Client secret del PaymentIntent para confirmación en el cliente.
     */
    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    /**
     * Monto total del pago en la unidad menor de la moneda.
     * Ejemplo: Para USD, $10.50 = 1050 centavos
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Moneda del pago (ISO 4217).
     * Ejemplo: "usd", "mxn", "eur"
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * Comisión de la plataforma (application_fee_amount en Stripe).
     * Valor en la unidad menor de la moneda.
     */
    @Column(name = "platform_fee", precision = 12, scale = 2)
    private BigDecimal platformFee;

    /**
     * Porcentaje de comisión aplicado.
     */
    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private BigDecimal platformFeePercentage;

    /**
     * Monto neto que recibe el usuario (amount - platform_fee).
     */
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    /**
     * Estado del pago.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * Método de pago utilizado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", length = 20)
    private PaymentMethodType paymentMethodType;

    /**
     * Últimos 4 dígitos de la tarjeta (si aplica).
     */
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    /**
     * Marca de la tarjeta (Visa, Mastercard, etc.).
     */
    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    /**
     * Email del cliente que realizó el pago.
     */
    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    /**
     * Nombre del cliente que realizó el pago.
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Descripción del pago.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Clave de idempotencia usada en la creación.
     * Evita duplicar pagos en caso de retry.
     */
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    /**
     * ID del Charge en Stripe (ch_xxxxx).
     * Se obtiene del webhook después de confirmar el pago.
     */
    @Column(name = "charge_id", length = 100)
    private String chargeId;

    /**
     * Indica si el pago fue reembolsado.
     */
    @Column(name = "refunded", nullable = false)
    private Boolean refunded;

    /**
     * Monto total reembolsado (si aplica).
     */
    @Column(name = "amount_refunded", precision = 12, scale = 2)
    private BigDecimal amountRefunded;

    /**
     * ID del Refund en Stripe (re_xxxxx).
     */
    @Column(name = "refund_id", length = 100)
    private String refundId;

    /**
     * Monto del último reembolso.
     */
    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    /**
     * Razón del reembolso.
     */
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    /**
     * Mensaje de error en caso de fallo.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Metadatos adicionales (JSON).
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Fecha de confirmación del pago.
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Estados posibles de un pago
     */
    public enum PaymentStatus {
        /**
         * PaymentIntent creado, esperando confirmación
         */
        PENDING,

        /**
         * Pago confirmado y exitoso
         */
        SUCCESS,

        /**
         * Pago confirmado y exitoso (alias de SUCCESS)
         */
        SUCCEEDED,

        /**
         * Pago rechazado/fallido
         */
        FAILED,

        /**
         * Pago cancelado antes de confirmarse
         */
        CANCELED,

        /**
         * Pago reembolsado
         */
        REFUNDED,

        /**
         * Procesando (estado intermedio)
         */
        PROCESSING,

        /**
         * Requiere acción adicional (3D Secure, etc.)
         */
        REQUIRES_ACTION
    }

    /**
     * Tipos de método de pago
     */
    public enum PaymentMethodType {
        CARD,
        OXXO,
        SPEI,
        BANK_TRANSFER,
        WALLET,
        OTHER
    }
}
