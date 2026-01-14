package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para registrar IPs de registro y evitar abusos
 * Previene que usuarios creen múltiples cuentas desde la misma IP
 */
@Data
@Entity
@Table(name = "tbl_registro_ips", schema = "ccdiad", indexes = {
    @Index(name = "idx_registro_ips_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_registro_ips_created_at", columnList = "createdAt")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroIP {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 45) // Soporta IPv4 e IPv6
    private String ipAddress;

    @ManyToOne
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false)
    private String email; // Email del negocio registrado

    @Column(nullable = false, length = 100)
    private String userAgent; // Navegador/dispositivo

    @Column(length = 100)
    private String pais; // País detectado

    @Column(length = 100)
    private String ciudad; // Ciudad detectada

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean esPrueba = true; // Si fue registro de prueba

    @Column(nullable = false)
    private boolean activo = true; // Si el negocio está activo
}
