package com.reservas.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "Nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "Apellido paterno es requerido")
    @Size(max = 100, message = "Apellido paterno no puede exceder 100 caracteres")
    private String apellidoPaterno;

    @Size(max = 100, message = "Apellido materno no puede exceder 100 caracteres")
    private String apellidoMaterno;

    @Email(message = "Email debe ser válido")
    @Size(max = 255, message = "Email no puede exceder 255 caracteres")
    private String email;

    @Size(max = 20, message = "Teléfono no puede exceder 20 caracteres")
    private String telefono;

    private LocalDate fechaNacimiento;

    @Size(max = 50, message = "Género no puede exceder 50 caracteres")
    private String genero;

    @Size(max = 1000, message = "Notas no pueden exceder 1000 caracteres")
    private String notas;
}
