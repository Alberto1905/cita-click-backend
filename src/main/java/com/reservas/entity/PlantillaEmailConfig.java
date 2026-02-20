package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Configuración de plantilla de email personalizada por negocio
 * Permite personalizar colores y textos de los emails
 */
@Entity
@Table(name = "plantilla_email_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaEmailConfig {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @OneToOne
    @JoinColumn(name = "negocio_id", nullable = false, unique = true)
    private Negocio negocio;

    /**
     * Color primario en formato hexadecimal (ej: #1E40AF)
     * Usado para headers, botones principales
     */
    @Column(name = "color_primario", length = 7)
    private String colorPrimario;

    /**
     * Color secundario en formato hexadecimal
     * Usado para acentos, links, detalles
     */
    @Column(name = "color_secundario", length = 7)
    private String colorSecundario;

    /**
     * Color de fondo en formato hexadecimal
     * Usado para el fondo del email
     */
    @Column(name = "color_fondo", length = 7)
    private String colorFondo;

    /**
     * Mensaje de bienvenida personalizado
     * Ejemplo: "¡Hola! Gracias por confiar en nosotros"
     */
    @Column(name = "mensaje_bienvenida", length = 500)
    private String mensajeBienvenida;

    /**
     * Firma personalizada
     * Ejemplo: "Equipo de Salón Belleza Total"
     */
    @Column(name = "firma", length = 300)
    private String firma;

    /**
     * Información de contacto adicional
     * Ejemplo: "Teléfono: +52 33 1234 5678 | Dirección: Calle Principal 123"
     */
    @Column(name = "info_contacto", length = 500)
    private String infoContacto;

    /**
     * Tipo de diseño de plantilla base
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "diseno_base", nullable = false)
    @Builder.Default
    private TipoDiseno disenoBase = TipoDiseno.CLASICO;

    /**
     * Indica si la plantilla personalizada está activa
     * Si es false, se usa la plantilla por defecto
     */
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private boolean activa = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Tipos de diseño de plantilla disponibles
     */
    public enum TipoDiseno {
        CLASICO,      // Diseño tradicional con header y footer
        MODERNO,      // Diseño limpio y minimalista
        MINIMALISTA   // Diseño ultra simple
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        // Valores por defecto si no se especifican
        if (colorPrimario == null) {
            colorPrimario = "#1E40AF"; // Azul por defecto
        }
        if (colorSecundario == null) {
            colorSecundario = "#3B82F6"; // Azul claro por defecto
        }
        if (colorFondo == null) {
            colorFondo = "#F3F4F6"; // Gris claro por defecto
        }
    }
}
