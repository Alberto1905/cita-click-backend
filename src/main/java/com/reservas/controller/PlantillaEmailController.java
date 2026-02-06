package com.reservas.controller;

import com.reservas.dto.request.PlantillaEmailConfigRequest;
import com.reservas.dto.response.ApiResponse;
import com.reservas.dto.response.PlantillaEmailConfigResponse;
import com.reservas.service.FileStorageService;
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
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private FileStorageService fileStorageService;

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

    @DeleteMapping("/logo")
    @Operation(summary = "Eliminar logo", description = "Elimina el logo de la plantilla de email")
    public ResponseEntity<ApiResponse<PlantillaEmailConfigResponse>> eliminarLogo(Authentication auth) {
        try {
            PlantillaEmailConfigResponse config = plantillaEmailService.eliminarLogo(auth.getName());
            return ResponseEntity.ok(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(true)
                    .message("Logo eliminado exitosamente")
                    .data(config)
                    .build());
        } catch (Exception e) {
            log.error("Error al eliminar logo", e);
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

    @PostMapping("/upload-logo")
    @Operation(summary = "Subir logo", description = "Sube un archivo de logo y actualiza la configuración")
    public ResponseEntity<ApiResponse<PlantillaEmailConfigResponse>> subirLogo(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            // Subir archivo y obtener URL
            String logoUrl = fileStorageService.storeFile(file);

            // Actualizar configuración con nueva URL del logo
            PlantillaEmailConfigRequest request = new PlantillaEmailConfigRequest();
            request.setLogoUrl(logoUrl);
            PlantillaEmailConfigResponse config = plantillaEmailService.guardarConfiguracion(auth.getName(), request);

            return ResponseEntity.ok(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(true)
                    .message("Logo subido exitosamente")
                    .data(config)
                    .build());
        } catch (Exception e) {
            log.error("Error al subir logo", e);
            return ResponseEntity.badRequest().body(ApiResponse.<PlantillaEmailConfigResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
