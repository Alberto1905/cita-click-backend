package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    @NotBlank(message = "Token de Google es requerido")
    private String idToken; // ID Token de Google

    // Datos opcionales del negocio (para registro)
    private String nombreNegocio;
    private String tipoNegocio;
    private String plan; // Plan de suscripción (starter, professional, enterprise)
}
