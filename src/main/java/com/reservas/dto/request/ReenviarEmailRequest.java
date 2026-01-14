package com.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReenviarEmailRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;
}
