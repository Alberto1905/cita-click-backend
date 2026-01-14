package com.reservas.billing.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank(message = "Usuario ID es requerido")
    private String usuarioId;

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email inválido")
    private String email;

    private String name;
    private String phone;
    private java.util.Map<String, String> metadata;
}
