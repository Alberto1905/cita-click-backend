package com.reservas.payments.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /**
     * ID del pago en la base de datos (usado por el controlador).
     */
    private String paymentId;

    /**
     * ID del PaymentIntent en Stripe (usado internamente).
     */
    private String paymentIntentId;

    /**
     * Monto a reembolsar. Si es null, se reembolsa el total.
     */
    @DecimalMin(value = "0.01", message = "El monto mínimo es 0.01")
    private BigDecimal amount;

    /**
     * Razón del reembolso.
     */
    @Size(max = 500)
    private String reason;

    private java.util.Map<String, String> metadata;
}
