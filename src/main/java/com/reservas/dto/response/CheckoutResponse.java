package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de sesión de checkout creada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private String sessionId;
    private String clientSecret; // Para Stripe Embedded Checkout
    private String url; // URL de checkout (para Stripe Hosted Checkout)
    private String plan;
    private String monto;
    private String moneda;
}
