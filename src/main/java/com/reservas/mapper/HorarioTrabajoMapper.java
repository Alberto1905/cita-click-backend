package com.reservas.mapper;

import com.reservas.dto.response.HorarioTrabajoResponse;
import com.reservas.entity.HorarioTrabajo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre HorarioTrabajo (entidad) y HorarioTrabajoResponse (DTO)
 */
@Component
public class HorarioTrabajoMapper {

    /**
     * Convierte una entidad HorarioTrabajo a DTO
     */
    public HorarioTrabajoResponse toResponse(HorarioTrabajo horario) {
        if (horario == null) {
            return null;
        }

        return HorarioTrabajoResponse.builder()
                .id(horario.getId())
                .diaSemana(horario.getDiaSemana())
                .nombreDia(HorarioTrabajoResponse.obtenerNombreDia(horario.getDiaSemana()))
                .horaApertura(horario.getHoraApertura())
                .horaCierre(horario.getHoraCierre())
                .activo(horario.isActivo())
                .build();
    }

    /**
     * Convierte una lista de entidades a lista de DTOs
     */
    public List<HorarioTrabajoResponse> toResponseList(List<HorarioTrabajo> horarios) {
        if (horarios == null) {
            return List.of();
        }

        return horarios.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
