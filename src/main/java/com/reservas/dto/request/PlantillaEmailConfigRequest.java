package com.reservas.dto.request;

import com.reservas.entity.PlantillaEmailConfig;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar configuración de plantilla de email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaEmailConfigRequest {

    /**
     * URL del logo (puede ser null si no se ha subido)
     */
    @Size(max = 500, message = "La URL del logo no puede exceder 500 caracteres")
    private String logoUrl;

    /**
     * Color primario en formato hexadecimal (#RRGGBB)
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color primario debe estar en formato hexadecimal (#RRGGBB)")
    private String colorPrimario;

    /**
     * Color secundario en formato hexadecimal (#RRGGBB)
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color secundario debe estar en formato hexadecimal (#RRGGBB)")
    private String colorSecundario;

    /**
     * Color de fondo en formato hexadecimal (#RRGGBB)
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color de fondo debe estar en formato hexadecimal (#RRGGBB)")
    private String colorFondo;

    /**
     * Mensaje de bienvenida personalizado
     */
    @Size(max = 500, message = "El mensaje de bienvenida no puede exceder 500 caracteres")
    private String mensajeBienvenida;

    /**
     * Firma personalizada
     */
    @Size(max = 300, message = "La firma no puede exceder 300 caracteres")
    private String firma;

    /**
     * Información de contacto adicional
     */
    @Size(max = 500, message = "La información de contacto no puede exceder 500 caracteres")
    private String infoContacto;

    /**
     * Tipo de diseño de plantilla base
     */
    private PlantillaEmailConfig.TipoDiseno disenoBase;

    /**
     * Indica si la plantilla personalizada está activa
     */
    private Boolean activa;
}
