package com.reservas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para invitar un nuevo usuario al negocio
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitarUsuarioRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El apellido paterno es requerido")
    private String apellidoPaterno;

    private String apellidoMaterno;

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email no válido")
    private String email;

    private String telefono;

    @NotBlank(message = "El rol es requerido")
    private String rol; // owner, admin, empleado, recepcionista

    private String passwordTemporal; // Opcional, se generará automáticamente si no se provee
}
