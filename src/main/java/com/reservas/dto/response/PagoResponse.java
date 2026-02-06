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

    /**
     * Convierte una entidad Pago a PagoResponse
     */
    public static PagoResponse fromEntity(com.reservas.entity.Pago pago) {
        if (pago == null) {
            return null;
        }

        return PagoResponse.builder()
                .id(pago.getId().toString())
                .plan(pago.getPlan())
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .estado(pago.getEstado())
                .metodoPago(pago.getMetodoPago())
                .descripcion(pago.getDescripcion())
                .periodoInicio(pago.getPeriodoInicio())
                .periodoFin(pago.getPeriodoFin())
                .fechaCreacion(pago.getFechaCreacion())
                .fechaCompletado(pago.getFechaCompletado())
                .facturaUrl(pago.getFacturaUrl())
                .errorMensaje(pago.getErrorMensaje())
                .build();
    }
}
