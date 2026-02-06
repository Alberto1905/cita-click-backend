package com.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de límites del plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanLimitesDTO {

    private String tipoPlan;
    private String nombrePlan;
    private Integer maxUsuarios;
    private Integer maxClientes;
    private Integer maxCitasMes;
    private Integer maxServicios;
    private boolean emailRecordatoriosHabilitado;
    private boolean smsWhatsappHabilitado;
    private boolean reportesAvanzadosHabilitado;
    private boolean personalizacionEmailHabilitado;
    private boolean soportePrioritario;

    // Helpers para mostrar "Ilimitado" en frontend
    public String getMaxUsuariosLabel() {
        return maxUsuarios == -1 ? "Ilimitado" : String.valueOf(maxUsuarios);
    }

    public String getMaxClientesLabel() {
        return maxClientes == -1 ? "Ilimitado" : String.valueOf(maxClientes);
    }

    public String getMaxCitasMesLabel() {
        return maxCitasMes == -1 ? "Ilimitado" : String.valueOf(maxCitasMes);
    }

    public String getMaxServiciosLabel() {
        return maxServicios == -1 ? "Ilimitado" : String.valueOf(maxServicios);
    }
}
