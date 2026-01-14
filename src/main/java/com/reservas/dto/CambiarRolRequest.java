package com.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambiar el rol de un usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarRolRequest {

    @NotBlank(message = "El rol es requerido")
    private String rol; // owner, admin, empleado, recepcionista
}
