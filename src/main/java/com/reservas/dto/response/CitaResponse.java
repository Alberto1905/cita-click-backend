package com.reservas.dto.response;

import com.reservas.entity.Cita;
import com.reservas.entity.TipoRecurrencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponse {
    private String id;
    private LocalDateTime fechaHora;
    private String estado;
    private String notas;

    // Cliente info (nested object for frontend compatibility)
    private ClienteInfo cliente;

    // Servicio info (nested object for frontend compatibility)
    private ServicioInfo servicio;

    // Legacy fields for backward compatibility
    private UUID clienteId;
    private String clienteNombre;
    private UUID servicioId;
    private String servicioNombre;
    private Integer servicioDuracion;
    private BigDecimal servicioPrecio;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Campos de recurrencia
    private Boolean esRecurrente;
    private TipoRecurrencia tipoRecurrencia;
    private Integer intervaloRecurrencia;
    private LocalDateTime fechaFinRecurrencia;
    private Integer numeroOcurrencias;
    private String diasSemana;
    private String citaPadreId;
    private LocalDateTime fechaFin;
    private BigDecimal precio;
    private Boolean pagado;
    private LocalDateTime fechaPago;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteInfo {
        private UUID id;
        private String nombre;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private String telefono;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioInfo {
        private UUID id;
        private String nombre;
        private Integer duracion; // Frontend expects "duracion"
        private Integer duracionMinutos; // Keep for compatibility
        private BigDecimal precio;
    }

    public static CitaResponse fromEntity(Cita cita) {
        String clienteNombre = cita.getCliente().getNombre() + " " + cita.getCliente().getApellidoPaterno();
        if (cita.getCliente().getApellidoMaterno() != null) {
            clienteNombre += " " + cita.getCliente().getApellidoMaterno();
        }

        // Build nested cliente object
        ClienteInfo clienteInfo = ClienteInfo.builder()
                .id(cita.getCliente().getId())
                .nombre(cita.getCliente().getNombre())
                .apellidoPaterno(cita.getCliente().getApellidoPaterno())
                .apellidoMaterno(cita.getCliente().getApellidoMaterno())
                .telefono(cita.getCliente().getTelefono())
                .email(cita.getCliente().getEmail())
                .build();

        // Build nested servicio object
        ServicioInfo servicioInfo = ServicioInfo.builder()
                .id(cita.getServicio().getId())
                .nombre(cita.getServicio().getNombre())
                .duracion(cita.getServicio().getDuracionMinutos()) // Map duracionMinutos to duracion
                .duracionMinutos(cita.getServicio().getDuracionMinutos())
                .precio(cita.getServicio().getPrecio())
                .build();

        return CitaResponse.builder()
                .id(cita.getId())
                .fechaHora(cita.getFechaHora())
                .fechaFin(cita.getFechaFin())
                .estado(cita.getEstado().name())
                .notas(cita.getNotas())
                .precio(cita.getPrecio())
                // Nested objects
                .cliente(clienteInfo)
                .servicio(servicioInfo)
                // Legacy fields for backward compatibility
                .clienteId(cita.getCliente().getId())
                .clienteNombre(clienteNombre)
                .servicioId(cita.getServicio().getId())
                .servicioNombre(cita.getServicio().getNombre())
                .servicioDuracion(cita.getServicio().getDuracionMinutos())
                .servicioPrecio(cita.getServicio().getPrecio())
                // Recurrencia
                .esRecurrente(cita.isEsRecurrente())
                .tipoRecurrencia(cita.getTipoRecurrencia())
                .intervaloRecurrencia(cita.getIntervaloRecurrencia())
                .fechaFinRecurrencia(cita.getFechaFinRecurrencia())
                .numeroOcurrencias(cita.getNumeroOcurrencias())
                .diasSemana(cita.getDiasSemana())
                .citaPadreId(cita.getCitaPadreId())
                // Pago
                .pagado(cita.isPagado())
                .fechaPago(cita.getFechaPago())
                // Metadata
                .createdAt(cita.getCreatedAt())
                .updatedAt(cita.getUpdatedAt())
                .build();
    }
}
