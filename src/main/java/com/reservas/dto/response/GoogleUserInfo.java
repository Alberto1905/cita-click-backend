package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para almacenar información del usuario de Google
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {

    private String googleId;       // Sub (Subject) - ID único de Google
    private String email;          // Email verificado
    private String nombre;         // Given name
    private String apellido;       // Family name
    private String nombreCompleto; // Full name
    private String imageUrl;       // Picture URL
    private boolean emailVerified; // Email verified
}
