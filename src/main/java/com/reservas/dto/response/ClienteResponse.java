package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {
    private UUID id;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String genero;
    private String notas;
    private Integer totalCitas;
}
