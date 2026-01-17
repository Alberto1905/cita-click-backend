package com.reservas.service;

import com.reservas.dto.request.DisponibilidadRequest;
import com.reservas.dto.response.DisponibilidadResponse;
import com.reservas.entity.*;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para calcular disponibilidad de horarios según duración de servicios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioTrabajoRepository horarioTrabajoRepository;
    private final DiaLibreRepository diaLibreRepository;
    private final CitaRepository citaRepository;

    private static final int INTERVALO_MINUTOS = 15; // Intervalos de 15 minutos

    /**
     * Obtiene los horarios disponibles para una fecha y servicio(s)
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse obtenerHorariosDisponibles(String email, DisponibilidadRequest request) {
        log.info("Calculando horarios disponibles para fecha: {} con {} servicios",
                request.getFecha(), request.getServicioIds().size());

        // Validar usuario y negocio
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // Validar que la fecha no sea pasada
        if (request.getFecha().isBefore(LocalDate.now())) {
            throw new BadRequestException("No se pueden crear citas en fechas pasadas");
        }

        // Validar y cargar servicios
        List<Servicio> servicios = new ArrayList<>();
        for (String servicioId : request.getServicioIds()) {
            Servicio servicio = servicioRepository.findById(UUID.fromString(servicioId))
                    .orElseThrow(() -> new NotFoundException("Servicio no encontrado: " + servicioId));

            if (!servicio.getNegocio().getId().equals(negocio.getId())) {
                throw new BadRequestException("El servicio no pertenece a tu negocio");
            }

            if (!servicio.isActivo()) {
                throw new BadRequestException("El servicio " + servicio.getNombre() + " no está activo");
            }

            servicios.add(servicio);
        }

        // Calcular duración total
        int duracionTotal = servicios.stream()
                .mapToInt(Servicio::getDuracionMinutos)
                .sum();

        log.info("Duración total de servicios: {} minutos", duracionTotal);

        // Verificar si es día libre
        boolean esDiaLibre = !diaLibreRepository.findByNegocioAndFecha(negocio, request.getFecha()).isEmpty();
        if (esDiaLibre) {
            log.info("La fecha {} es día libre", request.getFecha());
            return DisponibilidadResponse.builder()
                    .fecha(request.getFecha().toString())
                    .duracionTotal(duracionTotal)
                    .horariosDisponibles(new ArrayList<>())
                    .build();
        }

        // Obtener horario de trabajo para el día de la semana
        DayOfWeek diaSemana = request.getFecha().getDayOfWeek();
        // Convertir DayOfWeek a número (0=Lunes, 6=Domingo)
        int numeroDia = diaSemana.getValue() - 1; // getValue() retorna 1-7 (Lunes-Domingo), necesitamos 0-6

        List<HorarioTrabajo> horarios = horarioTrabajoRepository.findByNegocioAndDiaSemana(negocio, numeroDia);
        HorarioTrabajo horario = horarios.isEmpty() ? null : horarios.get(0);

        if (horario == null || !horario.isActivo()) {
            log.info("No hay horario de trabajo configurado para el día {}", numeroDia);
            return DisponibilidadResponse.builder()
                    .fecha(request.getFecha().toString())
                    .duracionTotal(duracionTotal)
                    .horariosDisponibles(new ArrayList<>())
                    .build();
        }

        // Obtener citas del día
        LocalDateTime inicioDia = request.getFecha().atStartOfDay();
        LocalDateTime finDia = request.getFecha().atTime(23, 59, 59);

        List<Cita> citasDelDia = citaRepository.findByNegocio(negocio).stream()
                .filter(c -> c.getFechaHora().isAfter(inicioDia) && c.getFechaHora().isBefore(finDia))
                .filter(c -> c.getEstado() != Cita.EstadoCita.CANCELADA)
                .filter(c -> request.getCitaIdExcluir() == null || !c.getId().equals(request.getCitaIdExcluir()))
                .collect(Collectors.toList());

        log.info("Encontradas {} citas existentes en la fecha", citasDelDia.size());

        // Generar horarios disponibles
        List<DisponibilidadResponse.HorarioDisponible> horariosDisponibles =
                generarHorariosDisponibles(horario, citasDelDia, duracionTotal, request.getFecha());

        log.info("Generados {} horarios disponibles", horariosDisponibles.size());

        return DisponibilidadResponse.builder()
                .fecha(request.getFecha().toString())
                .duracionTotal(duracionTotal)
                .horariosDisponibles(horariosDisponibles)
                .build();
    }

    /**
     * Genera los horarios disponibles basados en el horario de trabajo y citas existentes
     */
    private List<DisponibilidadResponse.HorarioDisponible> generarHorariosDisponibles(
            HorarioTrabajo horario,
            List<Cita> citasExistentes,
            int duracionMinutos,
            LocalDate fecha) {

        List<DisponibilidadResponse.HorarioDisponible> disponibles = new ArrayList<>();

        LocalTime horaActual = horario.getHoraApertura();
        LocalTime horaFin = horario.getHoraCierre();

        // Generar intervalos de tiempo
        while (horaActual.plusMinutes(duracionMinutos).isBefore(horaFin) ||
                horaActual.plusMinutes(duracionMinutos).equals(horaFin)) {

            LocalTime horaFinPropuesta = horaActual.plusMinutes(duracionMinutos);

            // Verificar si el horario está disponible (no traslapa con otras citas)
            LocalDateTime inicioDateTime = LocalDateTime.of(fecha, horaActual);
            LocalDateTime finDateTime = LocalDateTime.of(fecha, horaFinPropuesta);

            boolean estaDisponible = !hayTraslape(inicioDateTime, finDateTime, citasExistentes);

            if (estaDisponible) {
                // Determinar si es horario recomendado (entre 10:00 y 16:00)
                boolean esRecomendado = horaActual.isAfter(LocalTime.of(9, 59)) &&
                        horaActual.isBefore(LocalTime.of(16, 1));

                DisponibilidadResponse.HorarioDisponible horarioDisponible =
                        DisponibilidadResponse.HorarioDisponible.builder()
                                .horaInicio(horaActual)
                                .horaFin(horaFinPropuesta)
                                .etiqueta(String.format("%s - %s", horaActual, horaFinPropuesta))
                                .recomendado(esRecomendado)
                                .build();

                disponibles.add(horarioDisponible);
            }

            // Avanzar al siguiente intervalo
            horaActual = horaActual.plusMinutes(INTERVALO_MINUTOS);
        }

        return disponibles;
    }

    /**
     * Verifica si hay traslape entre un horario propuesto y las citas existentes
     */
    private boolean hayTraslape(LocalDateTime inicio, LocalDateTime fin, List<Cita> citas) {
        for (Cita cita : citas) {
            // Verificar traslape:
            // El inicio de la nueva cita está dentro de una cita existente
            // O el fin de la nueva cita está dentro de una cita existente
            // O la nueva cita envuelve completamente a una cita existente
            boolean traslapa = (inicio.isBefore(cita.getFechaFin()) && fin.isAfter(cita.getFechaHora()));

            if (traslapa) {
                return true;
            }
        }
        return false;
    }
}
