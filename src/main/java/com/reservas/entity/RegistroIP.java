package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para registrar IPs de registro y evitar abusos.
 *
 * Previene que usuarios creen múltiples cuentas desde la misma IP
 * y ayuda a detectar patrones de uso sospechosos.
 *
 * @author Cita Click
 */
@Data
@Entity
@Table(
    name = "tbl_registro_ips",
    schema = "ccdiad",
    indexes = {
        @Index(name = "idx_registro_ips_ip_address", columnList = "ip_address"),
        @Index(name = "idx_registro_ips_negocio_id", columnList = "negocio_id"),
        @Index(name = "idx_registro_ips_created_at", columnList = "created_at"),
        @Index(name = "idx_registro_ips_es_prueba", columnList = "es_prueba")
    }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroIP {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ip_address", nullable = false, length = 45) // Soporta IPv4 e IPv6
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_registro_ip_negocio"))
    private Negocio negocio;

    @Column(name = "email", nullable = false, length = 255)
    private String email; // Email del negocio registrado

    @Column(name = "user_agent", nullable = false, length = 500)
    private String userAgent; // Navegador/dispositivo

    @Column(name = "pais", length = 100)
    private String pais; // País detectado

    @Column(name = "ciudad", length = 100)
    private String ciudad; // Ciudad detectada

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "es_prueba", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean esPrueba = true; // Si fue registro de prueba

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean activo = true; // Si el negocio está activo
}
