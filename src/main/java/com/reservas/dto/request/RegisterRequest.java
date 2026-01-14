package com.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    private String email;

    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 6, message = "Contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "Nombre es requerido")
    private String nombre;

    @NotBlank(message = "Apellido paterno es requerido")
    private String apellidoPaterno;

    @NotBlank(message = "Apellido materno es requerido")
    private String apellidoMaterno;

    // Opcional - si no se proporciona, se crea automáticamente
    private String nombreNegocio;

    private String tipoNegocio;

    // Opcional - plan de suscripción (starter, professional, enterprise)
    private String plan;
}