package com.reservas.controller;

import com.reservas.dto.request.ServicioRequest;
import com.reservas.dto.response.ApiResponse;
import com.reservas.dto.response.ServicioResponse;
import com.reservas.service.ServicioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicios")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Slf4j
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServicioResponse>> crearServicio(
            @Valid @RequestBody ServicioRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            ServicioResponse servicio = servicioService.crearServicio(email, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<ServicioResponse>builder()
                            .success(true)
                            .message("Servicio creado exitosamente")
                            .data(servicio)
                            .build());
        } catch (Exception e) {
            log.error(" Error al crear servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ServicioResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServicioResponse>>> listarServicios(
            @RequestParam(required = false) Boolean activos,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            List<ServicioResponse> servicios = servicioService.listarServicios(email, activos);

            return ResponseEntity.ok(ApiResponse.<List<ServicioResponse>>builder()
                    .success(true)
                    .message("Servicios obtenidos exitosamente")
                    .data(servicios)
                    .build());
        } catch (Exception e) {
            log.error(" Error al listar servicios: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<ServicioResponse>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{servicioId}")
    public ResponseEntity<ApiResponse<ServicioResponse>> obtenerServicio(
            @PathVariable String servicioId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            ServicioResponse servicio = servicioService.obtenerServicio(email, servicioId);

            return ResponseEntity.ok(ApiResponse.<ServicioResponse>builder()
                    .success(true)
                    .message("Servicio obtenido exitosamente")
                    .data(servicio)
                    .build());
        } catch (Exception e) {
            log.error(" Error al obtener servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ServicioResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{servicioId}")
    public ResponseEntity<ApiResponse<ServicioResponse>> actualizarServicio(
            @PathVariable String servicioId,
            @Valid @RequestBody ServicioRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            ServicioResponse servicio = servicioService.actualizarServicio(email, servicioId, request);

            return ResponseEntity.ok(ApiResponse.<ServicioResponse>builder()
                    .success(true)
                    .message("Servicio actualizado exitosamente")
                    .data(servicio)
                    .build());
        } catch (Exception e) {
            log.error(" Error al actualizar servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ServicioResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{servicioId}")
    public ResponseEntity<ApiResponse<Void>> eliminarServicio(
            @PathVariable String servicioId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            servicioService.eliminarServicio(email, servicioId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Servicio eliminado exitosamente")
                    .build());
        } catch (Exception e) {
            log.error(" Error al eliminar servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
