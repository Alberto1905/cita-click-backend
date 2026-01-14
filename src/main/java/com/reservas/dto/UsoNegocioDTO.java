package com.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta del uso actual del negocio
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsoNegocioDTO {

    private String periodo; // "YYYY-MM"
    private Integer totalUsuarios;
    private Integer totalClientes;
    private Integer totalCitasMes;
    private Integer totalServicios;

    // Límites del plan
    private Integer limiteUsuarios;
    private Integer limiteClientes;
    private Integer limiteCitasMes;
    private Integer limiteServicios;

    // Porcentajes de uso
    private Double porcentajeUsuarios;
    private Double porcentajeClientes;
    private Double porcentajeCitasMes;
    private Double porcentajeServicios;

    // Flags de alerta
    private boolean alertaUsuarios; // >= 80%
    private boolean alertaClientes;
    private boolean alertaCitasMes;
    private boolean alertaServicios;

    // Helper para calcular porcentaje
    public static Double calcularPorcentaje(int actual, int limite) {
        if (limite == -1) return 0.0; // Ilimitado
        if (limite == 0) return 0.0;
        return (actual * 100.0) / limite;
    }

    // Helper para verificar si está en alerta
    public static boolean esAlerta(Double porcentaje) {
        return porcentaje >= 80.0;
    }
}
