package com.reservas.controller;

import com.reservas.dto.response.ApiResponse;
import com.reservas.dto.response.DashboardMetricasResponse;
import com.reservas.service.DashboardMetricasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para métricas del dashboard
 */
@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Slf4j
public class DashboardController {

    @Autowired
    private DashboardMetricasService dashboardMetricasService;

    @GetMapping("/metricas")
    public ResponseEntity<ApiResponse<DashboardMetricasResponse>> obtenerMetricas(Authentication auth) {
        try {
            log.info("Obteniendo métricas del dashboard para: {}", auth.getName());
            DashboardMetricasResponse metricas = dashboardMetricasService.obtenerMetricas(auth.getName());
            return ResponseEntity.ok(ApiResponse.<DashboardMetricasResponse>builder()
                    .success(true)
                    .message("Métricas obtenidas exitosamente")
                    .data(metricas)
                    .build());
        } catch (Exception e) {
            log.error("Error al obtener métricas del dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<DashboardMetricasResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
