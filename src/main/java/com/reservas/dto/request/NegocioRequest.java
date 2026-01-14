package com.reservas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegocioRequest {
    private String nombre;
    private String descripcion;
    private String telefono;
    private String email;
    private String tipo;

    // Legacy fields for backward compatibility
    private String domicilio;
    private String ciudad;
    private String pais;

    // Nested direccion object from frontend
    private DireccionRequest direccion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DireccionRequest {
        private String calle;
        private String colonia;
        private String ciudad;
        private String codigoPostal;
        private String estado;
        private String pais;
    }
}