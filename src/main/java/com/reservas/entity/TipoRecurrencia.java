package com.reservas.entity;

/**
 * Tipos de recurrencia para citas repetitivas
 */
public enum TipoRecurrencia {
    NO_RECURRENTE,      // Cita única
    DIARIA,             // Todos los días
    SEMANAL,            // Cada semana (mismo día)
    QUINCENAL,          // Cada 2 semanas
    MENSUAL,            // Cada mes (mismo día)
    TRIMESTRAL,         // Cada 3 meses
    PERSONALIZADO       // Intervalo personalizado
}
