package com.reservas.payments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resultado de un pago procesado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    /**
     * Indica si el pago fue exitoso.
     */
    private Boolean success;

    /**
     * PaymentIntent asociado.
     */
    private PaymentIntent paymentIntent;

    /**
     * ID del Charge generado (si el pago fue exitoso).
     */
    private String chargeId;

    /**
     * Mensaje descriptivo del resultado.
     */
    private String message;

    /**
     * Código de error (si hubo fallo).
     */
    private String errorCode;

    /**
     * Fecha de procesamiento.
     */
    private LocalDateTime processedAt;
}
