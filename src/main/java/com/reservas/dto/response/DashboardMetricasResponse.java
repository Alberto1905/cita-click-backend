package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response con métricas completas del dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricasResponse {

    // Ingresos
    private IngresosMetricas ingresos;

    // Citas
    private CitasMetricas citas;

    // Servicios
    private ServiciosMetricas servicios;

    // Tendencias
    private List<TendenciaData> tendenciaSemanal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresosMetricas {
        private BigDecimal ingresoMensual;
        private BigDecimal ingresoSemanal;
        private BigDecimal ingresoDiarioPromedio;
        private BigDecimal diferenciaMesAnterior; // Porcentaje de cambio
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitasMetricas {
        private Long totalMes;
        private Long totalSemana;
        private Long totalHoy;
        private String diaMayorDemanda; // Ej: "Lunes"
        private String horaMayorDemanda; // Ej: "14:00"
        private Map<String, Long> citasPorDia; // Últimos 7 días
        private Map<String, Long> citasPorHora; // Distribución horaria
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiciosMetricas {
        private List<ServicioPopular> serviciosMasSolicitados;
        private Integer totalServiciosActivos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioPopular {
        private String id;
        private String nombre;
        private Long cantidadCitas;
        private BigDecimal ingresoGenerado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendenciaData {
        private LocalDate fecha;
        private Long citas;
        private BigDecimal ingresos;
    }
}
