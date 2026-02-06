package com.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitud de confirmación de cita
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCitaRequest {

    @NotBlank(message = "El ID de la cita es requerido")
    private String citaId;

    @NotNull(message = "El canal de notificación es requerido")
    private CanalNotificacion canal; // WHATSAPP, SMS, AMBOS

    private boolean confirmarPago; // Si el pago ha sido confirmado

    public enum CanalNotificacion {
        WHATSAPP,
        SMS,
        AMBOS
    }
}
