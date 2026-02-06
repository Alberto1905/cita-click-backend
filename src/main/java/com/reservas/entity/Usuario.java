package com.reservas.entity;

import com.reservas.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un usuario del sistema.
 *
 * Un usuario pertenece a un negocio y tiene un rol específico (OWNER, ADMIN, EMPLEADO, RECEPCIONISTA).
 * Soporta autenticación local (email/password) y OAuth2 (Google).
 *
 * @author Cita Click
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "tbl_usuarios",
    schema = "ccdiad",
    indexes = {
        @Index(name = "idx_usuario_email", columnList = "email"),
        @Index(name = "idx_usuario_negocio_id", columnList = "negocio_id"),
        @Index(name = "idx_usuario_negocio_activo", columnList = "negocio_id, activo"),
        @Index(name = "idx_usuario_token_verificacion", columnList = "token_verificacion")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido_paterno", nullable = false, length = 100)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // OAuth2 fields
    @Column(name = "auth_provider", length = 20)
    private String authProvider; // 'local', 'google'

    @Column(name = "provider_id", length = 255)
    private String providerId; // ID del usuario en el proveedor OAuth

    @Column(name = "image_url", length = 500)
    private String imageUrl; // URL de la foto de perfil

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_negocio"))
    private Negocio negocio;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol; // 'owner', 'admin', 'empleado', 'recepcionista'

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean activo = true;

    // Email verification fields
    @Column(name = "email_verificado", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean emailVerificado = false;

    @Column(name = "token_verificacion", length = 255)
    private String tokenVerificacion;

    @Column(name = "token_verificacion_expira")
    private LocalDateTime tokenVerificacionExpira;

    // Trial period fields
    @Column(name = "trial_used", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean trialUsed = false;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    /**
     * Método helper para obtener el nombre completo del usuario.
     *
     * @return Nombre completo concatenado
     */
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

    /**
     * Verifica si el usuario usa autenticación local (email/password).
     *
     * @return true si usa autenticación local
     */
    public boolean isLocalAuth() {
        return "local".equalsIgnoreCase(authProvider);
    }

    /**
     * Verifica si el usuario usa OAuth2 (Google, etc.).
     *
     * @return true si usa OAuth2
     */
    public boolean isOAuthUser() {
        return authProvider != null && !authProvider.equalsIgnoreCase("local");
    }
}
