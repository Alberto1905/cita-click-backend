package com.reservas.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request para crear una cita con múltiples servicios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaMultipleServiciosRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private String clienteId;

    @NotEmpty(message = "Debe seleccionar al menos un servicio")
    private List<String> servicioIds;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime fechaHora;

    private String notas;

    private String usuarioId; // Opcional: asignar a un usuario específico
}
