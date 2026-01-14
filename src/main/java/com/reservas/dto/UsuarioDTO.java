package com.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respuesta de Usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    private UUID id;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private String telefono;
    private String rol;
    private boolean activo;
    private String authProvider;
    private String imageUrl;
    private LocalDateTime createdAt;

    // Nombre completo helper
    public String getNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder(nombre);
        if (apellidoPaterno != null && !apellidoPaterno.isEmpty()) {
            nombreCompleto.append(" ").append(apellidoPaterno);
        }
        if (apellidoMaterno != null && !apellidoMaterno.isEmpty()) {
            nombreCompleto.append(" ").append(apellidoMaterno);
        }
        return nombreCompleto.toString();
    }
}
