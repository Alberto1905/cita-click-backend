package com.reservas.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "Nombre es requerido")
    private String nombre;

    @NotBlank(message = "Apellido paterno es requerido")
    private String apellidoPaterno;

    private String apellidoMaterno;

    @Email(message = "Email debe ser válido")
    private String email;

    private String telefono;

    private LocalDate fechaNacimiento;

    private String genero; // Masculino, Femenino, Otro, Prefiero no decir

    private String notas;
}
