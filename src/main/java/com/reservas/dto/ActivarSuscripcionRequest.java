package com.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para activar una suscripción después de un pago exitoso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivarSuscripcionRequest {

    @NotBlank(message = "El ID de transacción es requerido")
    private String transaccionId;           // ID del pago de Stripe/PayPal

    private String metodoPago;              // stripe, paypal, transferencia

    private String plan;                    // Opcional: cambiar de plan al activar
}
