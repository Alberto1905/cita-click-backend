package com.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegocioRequest {
    @Size(max = 200, message = "Nombre no puede exceder 200 caracteres")
    private String nombre;

    @Size(max = 1000, message = "Descripción no puede exceder 1000 caracteres")
    private String descripcion;

    @Size(max = 20, message = "Teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Email(message = "Email debe ser válido")
    @Size(max = 255, message = "Email no puede exceder 255 caracteres")
    private String email;

    @Size(max = 100, message = "Tipo no puede exceder 100 caracteres")
    private String tipo;

    // Legacy fields for backward compatibility
    @Size(max = 500, message = "Domicilio no puede exceder 500 caracteres")
    private String domicilio;

    @Size(max = 100, message = "Ciudad no puede exceder 100 caracteres")
    private String ciudad;

    @Size(max = 100, message = "País no puede exceder 100 caracteres")
    private String pais;

    // Nested direccion object from frontend
    private DireccionRequest direccion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DireccionRequest {
        @Size(max = 300, message = "Calle no puede exceder 300 caracteres")
        private String calle;

        @Size(max = 100, message = "Colonia no puede exceder 100 caracteres")
        private String colonia;

        @Size(max = 100, message = "Ciudad no puede exceder 100 caracteres")
        private String ciudad;

        @Size(max = 10, message = "Código postal no puede exceder 10 caracteres")
        private String codigoPostal;

        @Size(max = 100, message = "Estado no puede exceder 100 caracteres")
        private String estado;

        @Size(max = 100, message = "País no puede exceder 100 caracteres")
        private String pais;
    }
}