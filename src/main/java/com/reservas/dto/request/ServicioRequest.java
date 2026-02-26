package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioRequest {

    @NotBlank(message = "Nombre del servicio es requerido")
    @Size(max = 200, message = "Nombre del servicio no puede exceder 200 caracteres")
    private String nombre;

    @Size(max = 1000, message = "Descripción no puede exceder 1000 caracteres")
    private String descripcion;

    @NotNull(message = "Precio es requerido")
    @Positive(message = "Precio debe ser mayor a cero")
    private BigDecimal precio;

    @NotNull(message = "Duración es requerida")
    @Positive(message = "Duración debe ser mayor a cero")
    private Integer duracionMinutos;

    private Boolean activo;
}
