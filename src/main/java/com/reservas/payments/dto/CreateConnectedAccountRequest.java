package com.reservas.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateConnectedAccountRequest {

    @NotBlank(message = "Usuario ID es requerido")
    private String usuarioId;

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "País es requerido")
    @Size(min = 2, max = 2, message = "País debe ser código ISO de 2 letras (ej: MX, US)")
    @Pattern(regexp = "[A-Z]{2}", message = "País debe estar en mayúsculas")
    private String country;

    /**
     * Tipo de negocio: individual o company
     */
    @Builder.Default
    private String businessType = "individual";

    private java.util.Map<String, String> metadata;
}
