package com.reservas.controller;

import com.reservas.dto.PlanLimitesDTO;
import com.reservas.dto.UsoNegocioDTO;
import com.reservas.dto.response.ApiResponse;
import com.reservas.entity.Usuario;
import com.reservas.security.JwtProvider;
import com.reservas.service.PlanLimitesService;
import com.reservas.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para consulta de límites de planes y uso del negocio
 * Endpoints para obtener información sobre el plan actual y su uso
 */
@RestController
@RequestMapping("/planes")
@Slf4j
public class PlanesController {

    @Autowired
    private PlanLimitesService planLimitesService;

    @Autowired
    private JwtProvider tokenProvider;

    /**
     * GET /api/planes/limites
     * Obtiene los límites del plan actual del negocio
     * Muestra qué características están habilitadas y los límites de recursos
     */
    @GetMapping("/limites")
    public ResponseEntity<ApiResponse<PlanLimitesDTO>> obtenerLimitesPlan(
            @RequestHeader("Authorization") String token) {

        log.info("📊 Solicitud para obtener límites del plan");

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        PlanLimitesDTO limites = planLimitesService.obtenerLimitesPorEmail(email);

        log.info(" Límites del plan obtenidos: {}", limites.getTipoPlan());

        return ResponseEntity.ok(ApiResponse.<PlanLimitesDTO>builder()
                .success(true)
                .message("Límites del plan obtenidos exitosamente")
                .data(limites)
                .build());
    }

    /**
     * GET /api/planes/uso
     * Obtiene el uso actual del negocio comparado con los límites del plan
     * Incluye porcentajes de uso y alertas cuando se alcanza el 80%
     */
    @GetMapping("/uso")
    public ResponseEntity<ApiResponse<UsoNegocioDTO>> obtenerUsoPlan(
            @RequestHeader("Authorization") String token) {

        log.info("📈 Solicitud para obtener uso del plan");

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        UsoNegocioDTO uso = planLimitesService.obtenerUsoPorEmail(email);

        log.info(" Uso del plan obtenido - Periodo: {}", uso.getPeriodo());

        return ResponseEntity.ok(ApiResponse.<UsoNegocioDTO>builder()
                .success(true)
                .message("Uso del plan obtenido exitosamente")
                .data(uso)
                .build());
    }

    /**
     * GET /api/planes/validar-funcionalidad/{funcionalidad}
     * Valida si una funcionalidad específica está habilitada en el plan actual
     * Útil para mostrar/ocultar features en el frontend
     */
    @GetMapping("/validar-funcionalidad/{funcionalidad}")
    public ResponseEntity<ApiResponse<Boolean>> validarFuncionalidad(
            @PathVariable String funcionalidad,
            @RequestHeader("Authorization") String token) {

        log.info("🔍 Validando funcionalidad: {}", funcionalidad);

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        boolean habilitada = planLimitesService.validarFuncionalidadPorEmail(email, funcionalidad);

        log.info(" Funcionalidad '{}' está: {}", funcionalidad, habilitada ? "HABILITADA" : "DESHABILITADA");

        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .message("Validación completada")
                .data(habilitada)
                .build());
    }
}
