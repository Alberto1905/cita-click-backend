package com.reservas.payments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un reembolso en el dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    /**
     * ID del reembolso en el proveedor (re_xxxxx en Stripe).
     */
    private String id;

    /**
     * ID del pago original.
     */
    private String paymentIntentId;

    /**
     * ID del charge reembolsado.
     */
    private String chargeId;

    /**
     * Monto reembolsado.
     */
    private BigDecimal amount;

    /**
     * Moneda del reembolso.
     */
    private String currency;

    /**
     * Razón del reembolso.
     */
    private String reason;

    /**
     * Estado del reembolso.
     */
    private RefundStatus status;

    /**
     * Metadatos.
     */
    private java.util.Map<String, String> metadata;

    /**
     * Fecha de creación.
     */
    private LocalDateTime createdAt;

    public enum RefundStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        CANCELED
    }
}
