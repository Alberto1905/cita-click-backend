package com.reservas.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request para obtener horarios disponibles según duración de servicio(s)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadRequest {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotEmpty(message = "Debe seleccionar al menos un servicio")
    private List<String> servicioIds;

    private String citaIdExcluir; // Para edición de citas existentes
}
