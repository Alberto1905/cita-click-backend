package com.reservas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioTrabajoRequest {
    private Integer diaSemana;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private boolean activo;
}