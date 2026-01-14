package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Response con horarios disponibles para una fecha y servicio(s)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadResponse {

    private String fecha;

    private Integer duracionTotal; // En minutos

    private List<HorarioDisponible> horariosDisponibles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HorarioDisponible {
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private String etiqueta; // Ej: "09:00 - 10:00"
        private Boolean recomendado; // Si está en horario pico o favorable
    }
}
