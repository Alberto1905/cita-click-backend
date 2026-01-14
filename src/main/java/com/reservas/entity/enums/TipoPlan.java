package com.reservas.entity.enums;

import lombok.Getter;

@Getter
public enum TipoPlan {
    BASICO("basico", "Básico", 299.00),
    PROFESIONAL("profesional", "Profesional", 699.00),
    PREMIUM("premium", "Premium", 1299.00);

    private final String codigo;
    private final String nombre;
    private final double precioMensual;

    TipoPlan(String codigo, String nombre, double precioMensual) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precioMensual = precioMensual;
    }

    public static TipoPlan fromCodigo(String codigo) {
        for (TipoPlan plan : values()) {
            if (plan.codigo.equalsIgnoreCase(codigo)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Plan no válido: " + codigo);
    }
}
