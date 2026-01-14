package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response para cita con múltiples servicios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaMultipleServiciosResponse {

    private CitaResponse cita;

    private List<ServicioInfo> servicios;

    private BigDecimal precioTotal;

    private Integer duracionTotal; // En minutos

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioInfo {
        private String id;
        private String nombre;
        private BigDecimal precio;
        private Integer duracionMinutos;
    }
}
