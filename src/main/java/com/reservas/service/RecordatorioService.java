package com.reservas.service;

import com.reservas.entity.Cita;
import com.reservas.entity.PlantillaEmailConfig;
import com.reservas.entity.Recordatorio;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.PlantillaEmailConfigRepository;
import com.reservas.repository.RecordatorioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

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

    @Autowired
    private PlantillaEmailConfigRepository plantillaEmailConfigRepository;

    /**
     * Crea recordatorios para una cita
     * @param cita Cita para la cual crear recordatorios
     */
    @Transactional
    public void crearRecordatoriosParaCita(Cita cita) {
        log.info("Creando recordatorios para cita: {}", cita.getId());

        // Recordatorio por Email (24 horas antes) - HABILITADO
        if (cita.getCliente().getEmail() != null && !cita.getCliente().getEmail().isBlank()) {
            Recordatorio recordatorioEmail = Recordatorio.builder()
                    .cita(cita)
                    .tipo(Recordatorio.TipoRecordatorio.EMAIL)
                    .enviado(false)
                    .build();
            recordatorioRepository.save(recordatorioEmail);
            log.debug("Recordatorio EMAIL creado para cita: {}", cita.getId());
        }

        // Recordatorio por SMS (2 horas antes) - DESHABILITADO HASTA TENER CLIENTES
        // Se activará cuando se configure Twilio y haya clientes reales
        /*
        if (cita.getCliente().getTelefono() != null && !cita.getCliente().getTelefono().isBlank()) {
            Recordatorio recordatorioSms = Recordatorio.builder()
                    .cita(cita)
                    .tipo(Recordatorio.TipoRecordatorio.SMS)
                    .enviado(false)
                    .build();
            recordatorioRepository.save(recordatorioSms);
            log.debug("Recordatorio SMS creado para cita: {}", cita.getId());
        }
        */
        log.debug("⚠️ Recordatorio SMS deshabilitado temporalmente para cita: {}", cita.getId());

        log.info("✅ Recordatorios creados para cita: {}", cita.getId());
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
     * Formatea una fecha en español (ej: "Lunes 20 de Enero, 2026")
     */
    private String formatearFecha(LocalDateTime fechaHora) {
        Locale spanish = new Locale("es", "MX");

        String diaSemana = fechaHora.getDayOfWeek().getDisplayName(TextStyle.FULL, spanish);
        diaSemana = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1);

        String mes = fechaHora.getMonth().getDisplayName(TextStyle.FULL, spanish);
        mes = mes.substring(0, 1).toUpperCase() + mes.substring(1);

        int dia = fechaHora.getDayOfMonth();
        int anio = fechaHora.getYear();

        return String.format("%s %d de %s, %d", diaSemana, dia, mes, anio);
    }

    /**
     * Formatea una hora (ej: "10:00 AM")
     */
    private String formatearHora(LocalDateTime fechaHora) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", new Locale("es", "MX"));
        return fechaHora.format(formatter).toUpperCase();
    }

    /**
     * Envía un recordatorio específico
     * @param recordatorio Recordatorio a enviar
     */
    private void enviarRecordatorio(Recordatorio recordatorio) {
        Cita cita = recordatorio.getCita();
        String nombreCliente = cita.getCliente().getNombre() + " " + cita.getCliente().getApellidoPaterno();
        String fechaCita = formatearFecha(cita.getFechaHora());
        String horaCita = formatearHora(cita.getFechaHora());
        String nombreServicio = cita.getServicio().getNombre();
        String nombreNegocio = cita.getNegocio().getNombre();

        boolean enviado = false;

        switch (recordatorio.getTipo()) {
            case EMAIL:
                if (cita.getCliente().getEmail() != null) {
                    // Cargar configuración de plantilla del negocio (colores, textos, diseño)
                    PlantillaEmailConfig emailConfig = plantillaEmailConfigRepository
                            .findByNegocio(cita.getNegocio())
                            .orElse(null);
                    enviado = emailService.enviarRecordatorioCita(
                            cita.getCliente().getEmail(),
                            nombreCliente,
                            fechaCita,
                            horaCita,
                            nombreServicio,
                            nombreNegocio,
                            emailConfig
                    );
                }
                break;

            case SMS:
                // DESHABILITADO TEMPORALMENTE - Se activará cuando se configure Twilio
                log.info("📱 Recordatorio SMS deshabilitado temporalmente para cita: {}", cita.getId());
                /*
                if (cita.getCliente().getTelefono() != null) {
                    enviado = smsService.enviarRecordatorioCita(
                            cita.getCliente().getTelefono(),
                            nombreCliente,
                            fechaCita + " " + horaCita,
                            nombreServicio
                    );
                }
                */
                break;

            case WHATSAPP:
                // DESHABILITADO TEMPORALMENTE - Próximamente disponible
                log.info("💬 Recordatorio WhatsApp deshabilitado temporalmente para cita: {}", cita.getId());
                /*
                if (cita.getCliente().getTelefono() != null) {
                    String mensaje = String.format(
                            "Hola %s, te recordamos tu cita de %s para el %s a las %s. ¡Te esperamos!",
                            nombreCliente,
                            nombreServicio,
                            fechaCita,
                            horaCita
                    );
                    enviado = smsService.enviarWhatsApp(cita.getCliente().getTelefono(), mensaje);
                }
                */
                break;
        }

        if (enviado) {
            recordatorio.setEnviado(true);
            recordatorio.setFechaEnvio(LocalDateTime.now());
            recordatorioRepository.save(recordatorio);
            log.info("✅ Recordatorio {} enviado para cita: {}", recordatorio.getTipo(), cita.getId());
        } else {
            log.warn("⚠️ No se pudo enviar recordatorio {} para cita: {}", recordatorio.getTipo(), cita.getId());
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
