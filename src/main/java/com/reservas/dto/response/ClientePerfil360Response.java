package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para la vista de perfil 360 del cliente
 * Contiene toda la información relevante del cliente en un solo lugar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientePerfil360Response {

    // Información básica del cliente
    private UUID id;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private String notas;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimaActualizacion;

    // Estadísticas de citas
    private EstadisticasCitas estadisticas;

    // Historial de citas
    private List<CitaResumen> historialCitas;

    // Próximas citas
    private List<CitaResumen> proximasCitas;

    // Servicios más utilizados
    private List<ServicioUtilizado> serviciosFrecuentes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasCitas {
        private Long totalCitas;
        private Long citasCompletadas;
        private Long citasCanceladas;
        private Long citasPendientes;
        private Long citasConfirmadas;
        private BigDecimal gastoTotal;
        private BigDecimal gastoPromedio;
        private LocalDateTime ultimaCita;
        private LocalDateTime proximaCita;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitaResumen {
        private String id;
        private LocalDateTime fechaHora;
        private LocalDateTime fechaFin;
        private String estado;
        private String servicioNombre;
        private BigDecimal precio;
        private String notas;
        private Boolean esRecurrente;
        private String tipoRecurrencia;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioUtilizado {
        private UUID servicioId;
        private String servicioNombre;
        private Long cantidadVeces;
        private BigDecimal gastoTotal;
        private LocalDateTime ultimaVez;
    }
}
