package com.reservas.scheduler;

import com.reservas.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler para ejecutar tareas automáticas relacionadas con suscripciones.
 * - Verifica y desactiva cuentas vencidas (3:00 AM diario)
 * - Envía notificaciones de vencimiento (9:00 AM diario)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuscripcionScheduler {

    private final SuscripcionService suscripcionService;

    /**
     * Verifica el estado de todas las suscripciones y desactiva las que hayan expirado.
     * Se ejecuta todos los días a las 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *") // Ejecutar a las 3:00 AM todos los días
    public void verificarSuscripciones() {
        log.info("========================================");
        log.info("Iniciando verificación de suscripciones: {}", LocalDateTime.now());
        log.info("========================================");

        try {
            suscripcionService.verificarSuscripcionesVencidas();
            log.info("Verificación de suscripciones completada exitosamente");
        } catch (Exception e) {
            log.error("Error durante la verificación de suscripciones", e);
        }

        log.info("========================================");
    }

    /**
     * Envía notificaciones a usuarios cuyas suscripciones están próximas a vencer.
     * - Notifica 1 día antes del fin de prueba
     * - Notifica 5 días antes del vencimiento mensual
     * Se ejecuta todos los días a las 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * *") // Ejecutar a las 9:00 AM todos los días
    public void enviarNotificaciones() {
        log.info("========================================");
        log.info("Iniciando envío de notificaciones de suscripción: {}", LocalDateTime.now());
        log.info("========================================");

        try {
            suscripcionService.enviarNotificaciones();
            log.info("Envío de notificaciones completado exitosamente");
        } catch (Exception e) {
            log.error("Error durante el envío de notificaciones", e);
        }

        log.info("========================================");
    }

    /**
     * OPCIONAL: Limpieza de registros IP antiguos (más de 90 días)
     * Se ejecuta el primer día de cada mes a las 2:00 AM
     */
    @Scheduled(cron = "0 0 2 1 * *") // Ejecutar el día 1 de cada mes a las 2:00 AM
    public void limpiarRegistrosIPAntiguos() {
        log.info("========================================");
        log.info("Iniciando limpieza de registros IP antiguos: {}", LocalDateTime.now());
        log.info("========================================");

        try {
            // Esta funcionalidad se puede implementar si se desea
            // suscripcionService.limpiarRegistrosIPAntiguos();
            log.info("Limpieza de registros IP completada (actualmente deshabilitada)");
        } catch (Exception e) {
            log.error("Error durante la limpieza de registros IP", e);
        }

        log.info("========================================");
    }
}
