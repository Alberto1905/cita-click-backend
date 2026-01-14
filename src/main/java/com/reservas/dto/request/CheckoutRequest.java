package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una sesión de checkout de Stripe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "El plan es requerido")
    @Pattern(regexp = "basico|profesional|premium", message = "Plan inválido. Debe ser: basico, profesional o premium")
    private String plan;

    // Opcional: Si viene de una landing page específica
    private String referencia;
}
