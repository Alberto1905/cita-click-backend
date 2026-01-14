package com.reservas.service;

import com.reservas.entity.Cita;
import com.reservas.entity.TipoRecurrencia;
import com.reservas.repository.CitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Servicio para gestionar citas recurrentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitaRecurrenteService {

    private final CitaRepository citaRepository;

    /**
     * Genera citas recurrentes basándose en la configuración de la cita padre
     */
    @Transactional
    public List<Cita> generarCitasRecurrentes(Cita citaPadre) {
        if (!citaPadre.isEsRecurrente() || citaPadre.getTipoRecurrencia() == TipoRecurrencia.NO_RECURRENTE) {
            log.warn("Intento de generar recurrencia para cita no recurrente: {}", citaPadre.getId());
            return List.of();
        }

        List<Cita> citasGeneradas = new ArrayList<>();
        LocalDateTime fechaActual = citaPadre.getFechaHora();
        int ocurrenciasGeneradas = 0;

        // Determinar límite de generación
        int maxOcurrencias = citaPadre.getNumeroOcurrencias() != null
            ? citaPadre.getNumeroOcurrencias()
            : 52; // Por defecto, máximo 52 ocurrencias (1 año si es semanal)

        LocalDateTime fechaLimite = citaPadre.getFechaFinRecurrencia() != null
            ? citaPadre.getFechaFinRecurrencia()
            : fechaActual.plusYears(1); // Por defecto, 1 año

        log.info("[Citas Recurrentes] Generando citas para tipo: {} desde {} hasta {}",
                 citaPadre.getTipoRecurrencia(), fechaActual, fechaLimite);

        while (ocurrenciasGeneradas < maxOcurrencias && fechaActual.isBefore(fechaLimite)) {
            // Calcular siguiente fecha según tipo de recurrencia
            fechaActual = calcularSiguienteFecha(fechaActual, citaPadre);

            if (fechaActual.isAfter(fechaLimite)) {
                break;
            }

            // Crear nueva cita
            Cita nuevaCita = crearCitaHija(citaPadre, fechaActual);
            citasGeneradas.add(nuevaCita);
            ocurrenciasGeneradas++;
        }

        // Guardar todas las citas generadas
        List<Cita> citasGuardadas = citaRepository.saveAll(citasGeneradas);

        log.info("[Citas Recurrentes]  {} citas generadas para cita padre: {}",
                 citasGuardadas.size(), citaPadre.getId());

        return citasGuardadas;
    }

    /**
     * Calcula la siguiente fecha según el tipo de recurrencia
     */
    private LocalDateTime calcularSiguienteFecha(LocalDateTime fechaActual, Cita citaPadre) {
        TipoRecurrencia tipo = citaPadre.getTipoRecurrencia();

        switch (tipo) {
            case DIARIA:
                return fechaActual.plusDays(1);

            case SEMANAL:
                if (citaPadre.getDiasSemana() != null && !citaPadre.getDiasSemana().isEmpty()) {
                    return calcularSiguienteDiaSemana(fechaActual, citaPadre.getDiasSemana());
                }
                return fechaActual.plusWeeks(1);

            case QUINCENAL:
                return fechaActual.plusWeeks(2);

            case MENSUAL:
                return fechaActual.plusMonths(1);

            case TRIMESTRAL:
                return fechaActual.plusMonths(3);

            case PERSONALIZADO:
                int intervalo = citaPadre.getIntervaloRecurrencia() != null
                    ? citaPadre.getIntervaloRecurrencia()
                    : 1;
                return fechaActual.plusDays(intervalo);

            default:
                return fechaActual.plusDays(1);
        }
    }

    /**
     * Calcula el siguiente día de la semana para recurrencia semanal personalizada
     * diasSemana formato: "LUN,MIE,VIE"
     */
    private LocalDateTime calcularSiguienteDiaSemana(LocalDateTime fechaActual, String diasSemana) {
        List<String> diasList = Arrays.asList(diasSemana.split(","));
        List<DayOfWeek> diasSemanaEnum = new ArrayList<>();

        // Convertir string a DayOfWeek
        for (String dia : diasList) {
            switch (dia.trim().toUpperCase()) {
                case "LUN": diasSemanaEnum.add(DayOfWeek.MONDAY); break;
                case "MAR": diasSemanaEnum.add(DayOfWeek.TUESDAY); break;
                case "MIE": diasSemanaEnum.add(DayOfWeek.WEDNESDAY); break;
                case "JUE": diasSemanaEnum.add(DayOfWeek.THURSDAY); break;
                case "VIE": diasSemanaEnum.add(DayOfWeek.FRIDAY); break;
                case "SAB": diasSemanaEnum.add(DayOfWeek.SATURDAY); break;
                case "DOM": diasSemanaEnum.add(DayOfWeek.SUNDAY); break;
            }
        }

        // Buscar el siguiente día válido
        LocalDateTime siguiente = fechaActual.plusDays(1);
        int intentos = 0;
        while (intentos < 7 && !diasSemanaEnum.contains(siguiente.getDayOfWeek())) {
            siguiente = siguiente.plusDays(1);
            intentos++;
        }

        return siguiente;
    }

    /**
     * Crea una cita hija basada en la cita padre
     */
    private Cita crearCitaHija(Cita citaPadre, LocalDateTime nuevaFecha) {
        long duracionMinutos = ChronoUnit.MINUTES.between(citaPadre.getFechaHora(), citaPadre.getFechaFin());

        return Cita.builder()
                .negocio(citaPadre.getNegocio())
                .cliente(citaPadre.getCliente())
                .usuario(citaPadre.getUsuario())
                .servicio(citaPadre.getServicio())
                .fechaHora(nuevaFecha)
                .fechaFin(nuevaFecha.plusMinutes(duracionMinutos))
                .estado(Cita.EstadoCita.PENDIENTE)
                .notas(citaPadre.getNotas())
                .precio(citaPadre.getPrecio())
                .recordatorioEnviado(false)
                .citaPadreId(citaPadre.getId())
                .esRecurrente(false) // Las hijas no generan más recurrencias
                .tipoRecurrencia(TipoRecurrencia.NO_RECURRENTE)
                .build();
    }

    /**
     * Cancela todas las citas futuras de una serie recurrente
     */
    @Transactional
    public int cancelarSerieRecurrente(String citaPadreId) {
        List<Cita> citasHijas = citaRepository.findByCitaPadreIdAndFechaHoraAfter(
            citaPadreId,
            LocalDateTime.now()
        );

        citasHijas.forEach(cita -> cita.setEstado(Cita.EstadoCita.CANCELADA));
        citaRepository.saveAll(citasHijas);

        log.info("[Citas Recurrentes]  Canceladas {} citas futuras de la serie: {}",
                 citasHijas.size(), citaPadreId);

        return citasHijas.size();
    }

    /**
     * Obtiene todas las citas de una serie recurrente
     */
    public List<Cita> obtenerSerieRecurrente(String citaPadreId) {
        return citaRepository.findByCitaPadreId(citaPadreId);
    }

    /**
     * Actualiza todas las citas futuras de una serie (excepto fecha/hora)
     */
    @Transactional
    public int actualizarSerieRecurrente(String citaPadreId, Cita cambios) {
        List<Cita> citasHijas = citaRepository.findByCitaPadreIdAndFechaHoraAfter(
            citaPadreId,
            LocalDateTime.now()
        );

        citasHijas.forEach(cita -> {
            if (cambios.getNotas() != null) cita.setNotas(cambios.getNotas());
            if (cambios.getPrecio() != null) cita.setPrecio(cambios.getPrecio());
            if (cambios.getEstado() != null) cita.setEstado(cambios.getEstado());
        });

        citaRepository.saveAll(citasHijas);

        log.info("[Citas Recurrentes]  Actualizadas {} citas de la serie: {}",
                 citasHijas.size(), citaPadreId);

        return citasHijas.size();
    }
}
