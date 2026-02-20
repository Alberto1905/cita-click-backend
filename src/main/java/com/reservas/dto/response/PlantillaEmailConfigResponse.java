package com.reservas.dto.response;

import com.reservas.entity.PlantillaEmailConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para configuración de plantilla de email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaEmailConfigResponse {

    private String id;
    private String negocioId;
    private String colorPrimario;
    private String colorSecundario;
    private String colorFondo;
    private String mensajeBienvenida;
    private String firma;
    private String infoContacto;
    private PlantillaEmailConfig.TipoDiseno disenoBase;
    private boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convierte una entidad PlantillaEmailConfig a DTO Response
     */
    public static PlantillaEmailConfigResponse fromEntity(PlantillaEmailConfig config) {
        return PlantillaEmailConfigResponse.builder()
                .id(config.getId())
                .negocioId(config.getNegocio().getId().toString())
                .colorPrimario(config.getColorPrimario())
                .colorSecundario(config.getColorSecundario())
                .colorFondo(config.getColorFondo())
                .mensajeBienvenida(config.getMensajeBienvenida())
                .firma(config.getFirma())
                .infoContacto(config.getInfoContacto())
                .disenoBase(config.getDisenoBase())
                .activa(config.isActiva())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
