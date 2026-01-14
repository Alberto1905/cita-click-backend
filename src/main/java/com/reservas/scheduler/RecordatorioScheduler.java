package com.reservas.scheduler;

import com.reservas.service.RecordatorioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para procesar recordatorios de citas periódicamente
 */
@Component
@Slf4j
public class RecordatorioScheduler {

    @Autowired
    private RecordatorioService recordatorioService;

    /**
     * Procesa recordatorios pendientes cada 30 minutos
     * Cron: segundos minutos horas día mes día-semana
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void procesarRecordatorios() {
        log.info("Iniciando procesamiento programado de recordatorios...");

        try {
            recordatorioService.procesarRecordatoriosPendientes();
            log.info("Procesamiento programado de recordatorios completado");
        } catch (Exception e) {
            log.error("Error en procesamiento programado de recordatorios: {}", e.getMessage(), e);
        }
    }

    /**
     * Tarea de prueba que se ejecuta cada minuto (para testing)
     * Comentar o eliminar en producción
     */
    // @Scheduled(cron = "0 * * * * *")
    // public void tareaTest() {
    //     log.debug("⏰ Tarea de prueba ejecutándose cada minuto");
    // }
}
