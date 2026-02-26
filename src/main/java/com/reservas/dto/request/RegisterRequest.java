package com.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres, una letra mayúscula y un número"
    )
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