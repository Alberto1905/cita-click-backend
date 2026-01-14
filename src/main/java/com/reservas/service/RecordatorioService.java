package com.reservas.service;

import com.reservas.entity.Cita;
import com.reservas.entity.Recordatorio;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.RecordatorioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestión de recordatorios de citas
 */
@Service
@Slf4j
public class RecordatorioService {

    @Autowired
    private RecordatorioRepository recordatorioRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private EmailService emailService;

    /**
     * Crea recordatorios para una cita
     * @param cita Cita para la cual crear recordatorios
     */
    @Transactional
    public void crearRecordatoriosParaCita(Cita cita) {
        log.info("Creando recordatorios para cita: {}", cita.getId());

        // Recordatorio por Email (24 horas antes)
        if (cita.getCliente().getEmail() != null && !cita.getCliente().getEmail().isBlank()) {
            Recordatorio recordatorioEmail = Recordatorio.builder()
                    .cita(cita)
                    .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                    .enviado(false)
                    .build();
            recordatorioRepository.save(recordatorioEmail);
            log.debug("Recordatorio EMAIL creado para cita: {}", cita.getId());
        }

        // Recordatorio por SMS (2 horas antes)
        if (cita.getCliente().getTelefono() != null && !cita.getCliente().getTelefono().isBlank()) {
            Recordatorio recordatorioSms = Recordatorio.builder()
                    .cita(cita)
                    .tipo(Recordatorio.TipoRecordatorio.SMS)
                    .enviado(false)
                    .build();
            recordatorioRepository.save(recordatorioSms);
            log.debug("Recordatorio SMS creado para cita: {}", cita.getId());
        }

        log.info(" Recordatorios creados para cita: {}", cita.getId());
    }

    /**
     * Procesa y envía recordatorios pendientes
     * Este método debe ser llamado por un scheduler periódicamente
     */
    @Transactional
    public void procesarRecordatoriosPendientes() {
        log.info("Procesando recordatorios pendientes...");

        // Obtener recordatorios no enviados
        List<Recordatorio> recordatoriosPendientes = recordatorioRepository.findByEnviadoFalse();
        log.info("Recordatorios pendientes encontrados: {}", recordatoriosPendientes.size());

        LocalDateTime ahora = LocalDateTime.now();

        for (Recordatorio recordatorio : recordatoriosPendientes) {
            try {
                Cita cita = recordatorio.getCita();

                // Determinar cuándo enviar según el tipo
                LocalDateTime momentoEnvio = calcularMomentoEnvio(cita.getFechaHora(), recordatorio.getTipo());

                // Solo enviar si ya es momento
                if (ahora.isAfter(momentoEnvio)) {
                    enviarRecordatorio(recordatorio);
                }
            } catch (Exception e) {
                log.error(" Error al procesar recordatorio {}: {}", recordatorio.getId(), e.getMessage());
            }
        }

        log.info(" Procesamiento de recordatorios completado");
    }

    /**
     * Envía un recordatorio específico
     * @param recordatorio Recordatorio a enviar
     */
    private void enviarRecordatorio(Recordatorio recordatorio) {
        Cita cita = recordatorio.getCita();
        String nombreCliente = cita.getCliente().getNombre() + " " + cita.getCliente().getApellidoPaterno();
        String fechaHora = cita.getFechaHora().toString();
        String nombreServicio = cita.getServicio().getNombre();

        boolean enviado = false;

        switch (recordatorio.getTipo()) {
            case EMAIL:
                if (cita.getCliente().getEmail() != null) {
                    enviado = emailService.enviarRecordatorioCita(
                            cita.getCliente().getEmail(),
                            nombreCliente,
                            fechaHora,
                            nombreServicio,
                            cita.getNegocio().getNombre()
                    );
                }
                break;

            case SMS:
                if (cita.getCliente().getTelefono() != null) {
                    enviado = smsService.enviarRecordatorioCita(
                            cita.getCliente().getTelefono(),
                            nombreCliente,
                            fechaHora,
                            nombreServicio
                    );
                }
                break;

            case WHATSAPP:
                if (cita.getCliente().getTelefono() != null) {
                    String mensaje = String.format(
                            "Hola %s, te recordamos tu cita de %s para el %s. ¡Te esperamos!",
                            nombreCliente,
                            nombreServicio,
                            fechaHora
                    );
                    enviado = smsService.enviarWhatsApp(cita.getCliente().getTelefono(), mensaje);
                }
                break;
        }

        if (enviado) {
            recordatorio.setEnviado(true);
            recordatorio.setFechaEnvio(LocalDateTime.now());
            recordatorioRepository.save(recordatorio);
            log.info(" Recordatorio {} enviado para cita: {}", recordatorio.getTipo(), cita.getId());
        } else {
            log.warn(" No se pudo enviar recordatorio {} para cita: {}", recordatorio.getTipo(), cita.getId());
        }
    }

    /**
     * Calcula el momento en que debe enviarse un recordatorio
     * @param fechaHoraCita Fecha y hora de la cita
     * @param tipo Tipo de recordatorio
     * @return Momento en que debe enviarse
     */
    private LocalDateTime calcularMomentoEnvio(LocalDateTime fechaHoraCita, Recordatorio.TipoRecordatorio tipo) {
        return switch (tipo) {
            case EMAIL -> fechaHoraCita.minusHours(24);  // 24 horas antes
            case SMS -> fechaHoraCita.minusHours(2);     // 2 horas antes
            case WHATSAPP -> fechaHoraCita.minusHours(4); // 4 horas antes
        };
    }
}
