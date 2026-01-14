package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteResponse {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String periodo; // DIARIO, SEMANAL, MENSUAL

    private Integer totalCitas;
    private Integer citasPendientes;
    private Integer citasConfirmadas;
    private Integer citasCompletadas;
    private Integer citasCanceladas;

    private BigDecimal ingresoTotal;
    private BigDecimal ingresoEstimado;

    private Integer clientesNuevos;
    private Integer clientesTotales;

    private String servicioMasPopular;
    private Integer servicioMasPopularCantidad;
}
