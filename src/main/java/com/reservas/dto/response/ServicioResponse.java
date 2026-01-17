package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioResponse {
    private UUID id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer duracionMinutos;
    private Boolean activo;
}
