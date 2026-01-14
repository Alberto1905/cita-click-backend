package com.reservas.controller;

import com.reservas.dto.request.CitaRequest;
import com.reservas.dto.request.CitaMultipleServiciosRequest;
import com.reservas.dto.request.DisponibilidadRequest;
import com.reservas.dto.response.ApiResponse;
import com.reservas.dto.response.CitaResponse;
import com.reservas.dto.response.CitaMultipleServiciosResponse;
import com.reservas.dto.response.DisponibilidadResponse;
import com.reservas.service.CitaService;
import com.reservas.service.CitaRecurrenteService;
import com.reservas.service.DisponibilidadService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/citas")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Slf4j
public class CitaController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private CitaRecurrenteService citaRecurrenteService;

    @Autowired
    private DisponibilidadService disponibilidadService;

    @PostMapping
    public ResponseEntity<ApiResponse<CitaResponse>> crearCita(
            @Valid @RequestBody CitaRequest request, Authentication auth) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CitaResponse>builder()
                    .success(true).message("Cita creada exitosamente")
                    .data(citaService.crearCita(auth.getName(), request)).build());
        } catch (Exception e) {
            log.error("Error al crear cita: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<CitaResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    /**
     * Endpoint para crear una cita con múltiples servicios
     */
    @PostMapping("/multiple-servicios")
    public ResponseEntity<ApiResponse<CitaMultipleServiciosResponse>> crearCitaConMultiplesServicios(
            @Valid @RequestBody CitaMultipleServiciosRequest request, Authentication auth) {
        try {
            log.info("Recibida solicitud para crear cita con {} servicios", request.getServicioIds().size());
            CitaMultipleServiciosResponse response = citaService.crearCitaConMultiplesServicios(auth.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CitaMultipleServiciosResponse>builder()
                    .success(true)
                    .message("Cita con múltiples servicios creada exitosamente")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error al crear cita con múltiples servicios: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<CitaMultipleServiciosResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Endpoint para obtener horarios disponibles según duración de servicio(s)
     */
    @PostMapping("/disponibilidad")
    public ResponseEntity<ApiResponse<DisponibilidadResponse>> obtenerDisponibilidad(
            @Valid @RequestBody DisponibilidadRequest request, Authentication auth) {
        try {
            log.info("Consultando disponibilidad para fecha: {} con {} servicios",
                    request.getFecha(), request.getServicioIds().size());
            DisponibilidadResponse response = disponibilidadService.obtenerHorariosDisponibles(auth.getName(), request);
            return ResponseEntity.ok(ApiResponse.<DisponibilidadResponse>builder()
                    .success(true)
                    .message("Horarios disponibles obtenidos exitosamente")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error al obtener disponibilidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<DisponibilidadResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CitaResponse>>> listarCitas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado,
            Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<List<CitaResponse>>builder()
                    .success(true).message("Citas obtenidas exitosamente")
                    .data(citaService.listarCitas(auth.getName(), fecha, estado)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<List<CitaResponse>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CitaResponse>> obtenerCita(@PathVariable String id, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<CitaResponse>builder()
                    .success(true).message("Cita obtenida exitosamente")
                    .data(citaService.obtenerCita(auth.getName(), id)).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<CitaResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CitaResponse>> actualizarCita(
            @PathVariable String id, @Valid @RequestBody CitaRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<CitaResponse>builder()
                    .success(true).message("Cita actualizada exitosamente")
                    .data(citaService.actualizarCita(auth.getName(), id, request)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<CitaResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<CitaResponse>> cambiarEstado(
            @PathVariable String id, @RequestParam String estado, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<CitaResponse>builder()
                    .success(true).message("Estado actualizado exitosamente")
                    .data(citaService.cambiarEstadoCita(auth.getName(), id, estado)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<CitaResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelarCita(@PathVariable String id, Authentication auth) {
        try {
            citaService.cancelarCita(auth.getName(), id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Cita cancelada exitosamente").build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<ApiResponse<List<LocalDateTime>>> obtenerDisponibilidad(
            @RequestParam String servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<List<LocalDateTime>>builder()
                    .success(true).message("Horarios disponibles obtenidos")
                    .data(citaService.obtenerHorariosDisponibles(auth.getName(), servicioId, fecha)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<List<LocalDateTime>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    // Endpoints para citas recurrentes

    @GetMapping("/{citaPadreId}/serie")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> obtenerSerieRecurrente(
            @PathVariable String citaPadreId, Authentication auth) {
        try {
            List<CitaResponse> serie = citaRecurrenteService.obtenerSerieRecurrente(citaPadreId)
                    .stream()
                    .map(CitaResponse::fromEntity)
                    .toList();
            return ResponseEntity.ok(ApiResponse.<List<CitaResponse>>builder()
                    .success(true)
                    .message("Serie de citas obtenida exitosamente")
                    .data(serie)
                    .build());
        } catch (Exception e) {
            log.error("[Citas Recurrentes] Error al obtener serie: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<List<CitaResponse>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{citaPadreId}/serie")
    public ResponseEntity<ApiResponse<Integer>> cancelarSerieRecurrente(
            @PathVariable String citaPadreId, Authentication auth) {
        try {
            int citasCanceladas = citaRecurrenteService.cancelarSerieRecurrente(citaPadreId);
            return ResponseEntity.ok(ApiResponse.<Integer>builder()
                    .success(true)
                    .message("Serie de citas cancelada exitosamente")
                    .data(citasCanceladas)
                    .build());
        } catch (Exception e) {
            log.error("[Citas Recurrentes] Error al cancelar serie: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Integer>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PatchMapping("/{citaPadreId}/serie")
    public ResponseEntity<ApiResponse<Integer>> actualizarSerieRecurrente(
            @PathVariable String citaPadreId,
            @Valid @RequestBody CitaRequest cambios,
            Authentication auth) {
        try {
            // Convertir el request a una entidad Cita con los cambios
            com.reservas.entity.Cita citaCambios = new com.reservas.entity.Cita();
            if (cambios.getNotas() != null) citaCambios.setNotas(cambios.getNotas());
            if (cambios.getPrecio() != null) citaCambios.setPrecio(cambios.getPrecio());
            if (cambios.getEstado() != null) {
                citaCambios.setEstado(com.reservas.entity.Cita.EstadoCita.valueOf(cambios.getEstado()));
            }

            int citasActualizadas = citaRecurrenteService.actualizarSerieRecurrente(citaPadreId, citaCambios);
            return ResponseEntity.ok(ApiResponse.<Integer>builder()
                    .success(true)
                    .message("Serie de citas actualizada exitosamente")
                    .data(citasActualizadas)
                    .build());
        } catch (Exception e) {
            log.error("[Citas Recurrentes] Error al actualizar serie: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Integer>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
