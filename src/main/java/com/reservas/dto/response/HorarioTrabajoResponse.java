package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO de respuesta para HorarioTrabajo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioTrabajoResponse {

    private UUID id;

    private Integer diaSemana; // 0-6 (Lunes-Domingo)

    private String nombreDia; // "Lunes", "Martes", etc.

    private LocalTime horaApertura;

    private LocalTime horaCierre;

    private boolean activo;

    /**
     * Convierte el número de día a nombre en español
     */
    public static String obtenerNombreDia(Integer diaSemana) {
        return switch (diaSemana) {
            case 0 -> "Lunes";
            case 1 -> "Martes";
            case 2 -> "Miércoles";
            case 3 -> "Jueves";
            case 4 -> "Viernes";
            case 5 -> "Sábado";
            case 6 -> "Domingo";
            default -> "Desconocido";
        };
    }
}
