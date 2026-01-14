package com.reservas.payments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un PaymentIntent en el dominio de la aplicación.
 *
 * Esta clase es un POJO (Plain Old Java Object) que abstrae
 * los detalles de implementación de Stripe u otro proveedor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntent {

    /**
     * ID del PaymentIntent en el proveedor (pi_xxxxx en Stripe).
     */
    private String id;

    /**
     * Monto del pago en la menor unidad de la moneda.
     */
    private BigDecimal amount;

    /**
     * Moneda del pago.
     */
    private String currency;

    /**
     * Estado del PaymentIntent.
     */
    private PaymentIntentStatus status;

    /**
     * ID de la cuenta conectada (para Stripe Connect).
     */
    private String connectedAccountId;

    /**
     * Comisión de la plataforma.
     */
    private BigDecimal platformFee;

    /**
     * Client secret para completar el pago en el frontend.
     */
    private String clientSecret;

    /**
     * Descripción del pago.
     */
    private String description;

    /**
     * Email del cliente.
     */
    private String customerEmail;

    /**
     * Nombre del cliente.
     */
    private String customerName;

    /**
     * Metadatos personalizados.
     */
    private java.util.Map<String, String> metadata;

    /**
     * Indica si requiere confirmación manual.
     */
    private Boolean requiresAction;

    /**
     * URL de la siguiente acción (3D Secure, etc.).
     */
    private String nextActionUrl;

    /**
     * Fecha de creación.
     */
    private LocalDateTime createdAt;

    /**
     * Fecha de confirmación.
     */
    private LocalDateTime confirmedAt;

    /**
     * Estados posibles de un PaymentIntent
     */
    public enum PaymentIntentStatus {
        REQUIRES_PAYMENT_METHOD,
        REQUIRES_CONFIRMATION,
        REQUIRES_ACTION,
        PROCESSING,
        REQUIRES_CAPTURE,
        CANCELED,
        SUCCEEDED
    }
}
