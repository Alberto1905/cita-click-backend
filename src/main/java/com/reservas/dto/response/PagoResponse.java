package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de información de pago
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {

    private String id;
    private String plan;
    private BigDecimal monto;
    private String moneda;
    private String estado;
    private String metodoPago;
    private LocalDateTime periodoInicio;
    private LocalDateTime periodoFin;
    private String descripcion;
    private String facturaUrl;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCompletado;
    private String errorMensaje;
}
