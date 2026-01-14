package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tbl_usuarios", schema = "ccdiad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, name = "apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @Column(unique = true, nullable = false)
    private String email;

    private String telefono;

    @Column(nullable = true)
    private String passwordHash;

    // OAuth2 fields
    @Column(name = "auth_provider")
    private String authProvider; // 'local', 'google', 'facebook', etc.

    @Column(name = "provider_id")
    private String providerId; // ID del usuario en el proveedor OAuth

    @Column(name = "image_url")
    private String imageUrl; // URL de la foto de perfil

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false)
    private String rol; // 'admin', 'empleado'

    @Column(columnDefinition = "boolean default true")
    @Builder.Default
    private boolean activo = true;

    // Email verification fields
    @Column(name = "email_verificado", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean emailVerificado = false;

    @Column(name = "token_verificacion")
    private String tokenVerificacion;

    @Column(name = "token_verificacion_expira")
    private LocalDateTime tokenVerificacionExpira;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}