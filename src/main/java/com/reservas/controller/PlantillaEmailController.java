package com.reservas.controller;

import com.reservas.dto.request.PlantillaEmailConfigRequest;
import com.reservas.dto.response.ApiResponse;
import com.reservas.dto.response.PlantillaEmailConfigResponse;
import com.reservas.service.PlantillaEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestionar plantillas de email personalizadas
 */
@RestController
@RequestMapping("/plantilla-email")
@Tag(name = "Plantilla Email", description = "Gestión de plantillas de email personalizadas")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class PlantillaEmailController {

    @Autowired
    private PlantillaEmailService plantillaEmailService;

    @GetMapping
    @Operation(summary = "Obtener configuración de plantilla", description = "Obtiene la configuración de plantilla de email del negocio")
    public ResponseEntity<ApiResponse<PlantillaEmailConfigResponse>> obtenerConfiguracion(Authentication auth) {
        try {
            PlantillaEmailConfigResponse config = plantillaEmailService.obtenerConfiguracion(auth.getName());
            return ResponseEntity.ok(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(true)
                    .message("Configuración obtenida exitosamente")
                    .data(config)
                    .build());
        } catch (Exception e) {
            log.error("Error al obtener configuración de plantilla", e);
            return ResponseEntity.badRequest().body(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping
    @Operation(summary = "Guardar configuración de plantilla", description = "Crea o actualiza la configuración de plantilla de email")
    public ResponseEntity<ApiResponse<PlantillaEmailConfigResponse>> guardarConfiguracion(
            @Valid @RequestBody PlantillaEmailConfigRequest request,
            Authentication auth) {
        try {
            PlantillaEmailConfigResponse config = plantillaEmailService.guardarConfiguracion(auth.getName(), request);
            return ResponseEntity.ok(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(true)
                    .message("Configuración guardada exitosamente")
                    .data(config)
                    .build());
        } catch (Exception e) {
            log.error("Error al guardar configuración de plantilla", e);
            return ResponseEntity.badRequest().body(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/restaurar")
    @Operation(summary = "Restaurar configuración por defecto", description = "Restaura la configuración de plantilla a valores por defecto")
    public ResponseEntity<ApiResponse<PlantillaEmailConfigResponse>> restaurarPorDefecto(Authentication auth) {
        try {
            PlantillaEmailConfigResponse config = plantillaEmailService.restaurarPorDefecto(auth.getName());
            return ResponseEntity.ok(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(true)
                    .message("Configuración restaurada a valores por defecto")
                    .data(config)
                    .build());
        } catch (Exception e) {
            log.error("Error al restaurar configuración por defecto", e);
            return ResponseEntity.badRequest().body(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
