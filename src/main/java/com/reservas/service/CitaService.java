package com.reservas.service;

import com.reservas.dto.request.CitaRequest;
import com.reservas.dto.request.CitaMultipleServiciosRequest;
import com.reservas.dto.response.CitaResponse;
import com.reservas.dto.response.CitaMultipleServiciosResponse;
import com.reservas.entity.Cita;
import com.reservas.entity.CitaServicio;
import com.reservas.entity.Cliente;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.entity.DiaLibre;
import com.reservas.entity.HorarioTrabajo;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.CitaServicioRepository;
import com.reservas.repository.ClienteRepository;
import com.reservas.repository.DiaLibreRepository;
import com.reservas.repository.HorarioTrabajoRepository;
import com.reservas.repository.ServicioRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CitaService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private CitaServicioRepository citaServicioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HorarioTrabajoRepository horarioTrabajoRepository;

    @Autowired
    private DiaLibreRepository diaLibreRepository;

    @Autowired
    private PlanLimitesService planLimitesService;

    @Autowired
    private CitaRecurrenteService citaRecurrenteService;

    @Transactional
    public CitaResponse crearCita(String email, CitaRequest request) {
        log.info("Creando cita para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // VALIDAR LÍMITE DE CITAS DEL MES
        com.reservas.entity.enums.TipoPlan plan = com.reservas.entity.enums.TipoPlan.fromCodigo(negocio.getPlan());
        planLimitesService.validarLimiteCitasMes(negocio.getId(), plan);

        // Validar cliente
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        if (!cliente.getNegocio().getId().equals(negocio.getId())) {
            throw new UnauthorizedException("El cliente no pertenece a tu negocio");
        }

        // Validar servicio
        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        if (!servicio.getNegocio().getId().equals(negocio.getId())) {
            throw new UnauthorizedException("El servicio no pertenece a tu negocio");
        }

        if (!servicio.isActivo()) {
            throw new BadRequestException("El servicio no está activo");
        }

        // Validar disponibilidad
        validarDisponibilidad(negocio, request.getFechaHora(), servicio.getDuracionMinutos(), null);

        // Calcular fecha fin basada en duración del servicio
        LocalDateTime fechaFin = request.getFechaHora().plusMinutes(servicio.getDuracionMinutos());

        // Determinar precio (del request o del servicio)
        java.math.BigDecimal precio = request.getPrecio() != null ? request.getPrecio() : servicio.getPrecio();

        Cita cita = Cita.builder()
                .fechaHora(request.getFechaHora())
                .fechaFin(fechaFin)
                .estado(Cita.EstadoCita.PENDIENTE)
                .notas(request.getNotas())
                .precio(precio)
                .cliente(cliente)
                .servicio(servicio)
                .negocio(negocio)
                .usuario(usuario)
                .build();

        // Configurar recurrencia si aplica
        if (Boolean.TRUE.equals(request.getEsRecurrente()) && request.getTipoRecurrencia() != null) {
            cita.setEsRecurrente(true);
            cita.setTipoRecurrencia(request.getTipoRecurrencia());
            cita.setIntervaloRecurrencia(request.getIntervaloRecurrencia());

            // Convertir LocalDate a LocalDateTime (fin del día)
            if (request.getFechaFinRecurrencia() != null) {
                cita.setFechaFinRecurrencia(request.getFechaFinRecurrencia().atTime(23, 59, 59));
            }

            cita.setNumeroOcurrencias(request.getNumeroOcurrencias());
            cita.setDiasSemana(request.getDiasSemana());

            log.info("[Citas Recurrentes] Configurando recurrencia tipo: {} para cita", request.getTipoRecurrencia());
        }

        cita = citaRepository.save(cita);
        log.info(" Cita creada: {} para cliente: {}", cita.getId(), cliente.getNombre());

        // Generar citas recurrentes si aplica
        if (cita.isEsRecurrente()) {
            List<com.reservas.entity.Cita> citasGeneradas = citaRecurrenteService.generarCitasRecurrentes(cita);
            log.info("[Citas Recurrentes]  Generadas {} citas adicionales", citasGeneradas.size());
        }

        // ACTUALIZAR USO
        planLimitesService.actualizarUso(negocio.getId());

        return mapToResponse(cita);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitas(String email, LocalDate fecha, String estado) {
        log.info("Listando citas para usuario: {} - Fecha: {} - Estado: {}", email, fecha, estado);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        List<Cita> citas;

        if (fecha != null && estado != null) {
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
            Cita.EstadoCita estadoEnum = Cita.EstadoCita.valueOf(estado.toUpperCase());
            citas = citaRepository.findByNegocioAndFechaHoraBetweenAndEstado(negocio, inicio, fin, estadoEnum);
        } else if (fecha != null) {
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
            citas = citaRepository.findByNegocioAndFechaHoraBetween(negocio, inicio, fin);
        } else if (estado != null) {
            Cita.EstadoCita estadoEnum = Cita.EstadoCita.valueOf(estado.toUpperCase());
            citas = citaRepository.findByNegocioAndEstado(negocio, estadoEnum);
        } else {
            citas = citaRepository.findByNegocioOrderByFechaHoraAsc(negocio);
        }

        log.info("Citas encontradas: {}", citas.size());

        return citas.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CitaResponse obtenerCita(String email, String citaId) {
        log.info("Obteniendo cita: {} para usuario: {}", citaId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        // Verificar que la cita pertenece al negocio del usuario
        if (!cita.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para acceder a esta cita");
        }

        return mapToResponse(cita);
    }

    @Transactional
    public CitaResponse actualizarCita(String email, String citaId, CitaRequest request) {
        log.info("Actualizando cita: {} para usuario: {}", citaId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        // Verificar que la cita pertenece al negocio del usuario
        if (!cita.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para actualizar esta cita");
        }

        // Validar servicio
        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        if (!servicio.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("El servicio no pertenece a tu negocio");
        }

        // Validar disponibilidad si cambió la fecha
        if (!cita.getFechaHora().equals(request.getFechaHora())) {
            validarDisponibilidad(cita.getNegocio(), request.getFechaHora(), servicio.getDuracionMinutos(), citaId);
        }

        cita.setFechaHora(request.getFechaHora());
        cita.setServicio(servicio);
        cita.setNotas(request.getNotas());

        cita = citaRepository.save(cita);
        log.info(" Cita actualizada: {}", citaId);

        return mapToResponse(cita);
    }

    @Transactional
    public CitaResponse cambiarEstadoCita(String email, String citaId, String nuevoEstado) {
        log.info("Cambiando estado de cita: {} a: {}", citaId, nuevoEstado);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        // Verificar que la cita pertenece al negocio del usuario
        if (!cita.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para modificar esta cita");
        }

        try {
            Cita.EstadoCita estadoEnum = Cita.EstadoCita.valueOf(nuevoEstado.toUpperCase());
            cita.setEstado(estadoEnum);
            cita = citaRepository.save(cita);
            log.info(" Estado de cita actualizado: {} -> {}", citaId, nuevoEstado);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Estado inválido: " + nuevoEstado);
        }

        return mapToResponse(cita);
    }

    @Transactional
    public void cancelarCita(String email, String citaId) {
        log.info("Cancelando cita: {} para usuario: {}", citaId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        // Verificar que la cita pertenece al negocio del usuario
        if (!cita.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para cancelar esta cita");
        }

        cita.setEstado(Cita.EstadoCita.CANCELADA);
        citaRepository.save(cita);
        log.info(" Cita cancelada: {}", citaId);
    }

    @Transactional(readOnly = true)
    public List<LocalDateTime> obtenerHorariosDisponibles(String email, String servicioId, LocalDate fecha) {
        log.info("Obteniendo horarios disponibles para servicio: {} en fecha: {}", servicioId, fecha);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        if (!servicio.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("El servicio no pertenece a tu negocio");
        }

        Negocio negocio = usuario.getNegocio();

        // 1. Verificar si es día libre
        List<DiaLibre> diasLibres = diaLibreRepository.findByNegocioAndFecha(negocio, fecha);
        if (!diasLibres.isEmpty()) {
            log.info("Fecha {} es día libre: {}", fecha, diasLibres.get(0).getRazon());
            return List.of();
        }

        // 2. Obtener horario de trabajo para ese día de la semana
        // DayOfWeek.getValue() retorna: MONDAY=1, TUESDAY=2, ..., SUNDAY=7
        // HorarioTrabajo.diaSemana usa: LUNES=0, MARTES=1, ..., DOMINGO=6
        // Conversión correcta: getValue() - 1, excepto Domingo que es caso especial
        int diaSemanaDayOfWeek = fecha.getDayOfWeek().getValue(); // 1-7
        int diaSemanaHorario;

        if (diaSemanaDayOfWeek == 7) {
            // Domingo: DayOfWeek=7 -> HorarioTrabajo=6
            diaSemanaHorario = 6;
        } else {
            // Lunes a Sábado: DayOfWeek=(1-6) -> HorarioTrabajo=(0-5)
            diaSemanaHorario = diaSemanaDayOfWeek - 1;
        }

        log.debug("Fecha: {}, DayOfWeek: {} ({}), Índice HorarioTrabajo: {}",
                fecha, fecha.getDayOfWeek(), diaSemanaDayOfWeek, diaSemanaHorario);

        List<HorarioTrabajo> horariosTrabajo = horarioTrabajoRepository
                .findByNegocioAndDiaSemana(negocio, diaSemanaHorario);

        if (horariosTrabajo.isEmpty()) {
            log.info("No hay horario de trabajo configurado para el día {} (índice: {})",
                    fecha.getDayOfWeek(), diaSemanaHorario);
            return List.of();
        }

        HorarioTrabajo horario = horariosTrabajo.get(0);
        if (!horario.isActivo()) {
            log.info("Horario de trabajo está inactivo para el día {} (índice: {})",
                    fecha.getDayOfWeek(), diaSemanaHorario);
            return List.of();
        }

        // 3. Obtener todas las citas programadas para ese día
        List<Cita> citasProgramadas = citaRepository.findByNegocioAndFecha(negocio, fecha);

        // 4. Generar slots disponibles
        List<LocalDateTime> horariosDisponibles = new ArrayList<>();
        int duracionMinutos = servicio.getDuracionMinutos();
        int intervaloMinutos = 30; // Intervalo entre citas disponibles

        LocalTime horaActual = horario.getHoraApertura();
        LocalTime horaCierre = horario.getHoraCierre();

        while (horaActual.plusMinutes(duracionMinutos).isBefore(horaCierre) ||
               horaActual.plusMinutes(duracionMinutos).equals(horaCierre)) {

            LocalDateTime slotPropuesto = LocalDateTime.of(fecha, horaActual);
            LocalDateTime slotFin = slotPropuesto.plusMinutes(duracionMinutos);

            // Verificar si el slot no se solapa con citas existentes
            boolean disponible = citasProgramadas.stream()
                    .filter(cita -> !cita.getEstado().equals(Cita.EstadoCita.CANCELADA))
                    .noneMatch(cita ->
                        // Verificar solapamiento: el nuevo slot no debe solaparse con citas existentes
                        (slotPropuesto.isBefore(cita.getFechaFin()) && slotFin.isAfter(cita.getFechaHora()))
                    );

            if (disponible) {
                horariosDisponibles.add(slotPropuesto);
            }

            horaActual = horaActual.plusMinutes(intervaloMinutos);
        }

        log.info(" Encontrados {} horarios disponibles para la fecha {}", horariosDisponibles.size(), fecha);
        return horariosDisponibles;
    }

    private void validarDisponibilidad(Negocio negocio, LocalDateTime fechaHora, Integer duracionMinutos, String citaIdExcluir) {
        LocalDateTime fechaFin = fechaHora.plusMinutes(duracionMinutos);

        // Buscar todas las citas del día para verificar solapamiento real
        LocalDate fecha = fechaHora.toLocalDate();
        List<Cita> citasDelDia = citaRepository.findByNegocioAndFecha(negocio, fecha);

        // Filtrar citas canceladas y la cita actual si estamos actualizando
        List<Cita> citasActivas = citasDelDia.stream()
                .filter(c -> !c.getEstado().equals(Cita.EstadoCita.CANCELADA))
                .filter(c -> citaIdExcluir == null || !c.getId().equals(citaIdExcluir))
                .collect(Collectors.toList());

        // Verificar si hay solapamiento REAL con alguna cita existente
        // Hay solapamiento si:
        // - La nueva cita empieza ANTES de que termine una existente Y
        // - La nueva cita termina DESPUÉS de que empiece una existente
        boolean haySolapamiento = citasActivas.stream()
                .anyMatch(cita -> {
                    // Solapamiento ocurre cuando:
                    // fechaHora < cita.fechaFin  Y  fechaFin > cita.fechaHora
                    boolean solapa = fechaHora.isBefore(cita.getFechaFin()) && fechaFin.isAfter(cita.getFechaHora());

                    if (solapa) {
                        log.debug("⚠ Solapamiento detectado - Nueva cita: {} a {}, Cita existente: {} a {}",
                                fechaHora, fechaFin, cita.getFechaHora(), cita.getFechaFin());
                    }

                    return solapa;
                });

        if (haySolapamiento) {
            throw new BadRequestException("Ya existe una cita programada que se solapa con este horario");
        }

        log.debug("✓ Horario disponible: {} a {} (sin solapamiento)", fechaHora, fechaFin);
    }

    /**
     * Crea una cita con múltiples servicios
     */
    @Transactional
    public CitaMultipleServiciosResponse crearCitaConMultiplesServicios(String email, CitaMultipleServiciosRequest request) {
        log.info("Creando cita con múltiples servicios para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // VALIDAR LÍMITE DE CITAS DEL MES
        com.reservas.entity.enums.TipoPlan plan = com.reservas.entity.enums.TipoPlan.fromCodigo(negocio.getPlan());
        planLimitesService.validarLimiteCitasMes(negocio.getId(), plan);

        // Validar cliente
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        if (!cliente.getNegocio().getId().equals(negocio.getId())) {
            throw new UnauthorizedException("El cliente no pertenece a tu negocio");
        }

        // Validar y cargar servicios
        List<Servicio> servicios = new ArrayList<>();
        for (String servicioId : request.getServicioIds()) {
            Servicio servicio = servicioRepository.findById(servicioId)
                    .orElseThrow(() -> new NotFoundException("Servicio no encontrado: " + servicioId));

            if (!servicio.getNegocio().getId().equals(negocio.getId())) {
                throw new UnauthorizedException("El servicio " + servicio.getNombre() + " no pertenece a tu negocio");
            }

            if (!servicio.isActivo()) {
                throw new BadRequestException("El servicio " + servicio.getNombre() + " no está activo");
            }

            servicios.add(servicio);
        }

        // Calcular duración total y precio total
        int duracionTotal = servicios.stream()
                .mapToInt(Servicio::getDuracionMinutos)
                .sum();

        java.math.BigDecimal precioTotal = servicios.stream()
                .map(Servicio::getPrecio)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Validar disponibilidad con la duración total
        validarDisponibilidad(negocio, request.getFechaHora(), duracionTotal, null);

        // Calcular fecha fin
        LocalDateTime fechaFin = request.getFechaHora().plusMinutes(duracionTotal);

        // Usar el primer servicio como servicio principal de la cita
        Servicio servicioPrincipal = servicios.get(0);

        // Crear la cita principal
        Cita cita = Cita.builder()
                .fechaHora(request.getFechaHora())
                .fechaFin(fechaFin)
                .estado(Cita.EstadoCita.PENDIENTE)
                .notas(request.getNotas())
                .precio(precioTotal)
                .cliente(cliente)
                .servicio(servicioPrincipal)
                .negocio(negocio)
                .usuario(usuario)
                .build();

        cita = citaRepository.save(cita);
        log.info(" Cita creada: {} para cliente: {} con {} servicios",
                cita.getId(), cliente.getNombre(), servicios.size());

        // Crear registros de CitaServicio para todos los servicios (incluyendo el principal)
        List<CitaServicio> citaServicios = new ArrayList<>();
        for (Servicio servicio : servicios) {
            CitaServicio citaServicio = CitaServicio.builder()
                    .cita(cita)
                    .servicio(servicio)
                    .precio(servicio.getPrecio())
                    .duracionMinutos(servicio.getDuracionMinutos())
                    .build();
            citaServicios.add(citaServicio);
        }

        citaServicioRepository.saveAll(citaServicios);
        log.info(" Guardados {} servicios para la cita", citaServicios.size());

        // ACTUALIZAR USO
        planLimitesService.actualizarUso(negocio.getId());

        // Construir respuesta
        List<CitaMultipleServiciosResponse.ServicioInfo> serviciosInfo = servicios.stream()
                .map(s -> CitaMultipleServiciosResponse.ServicioInfo.builder()
                        .id(s.getId())
                        .nombre(s.getNombre())
                        .precio(s.getPrecio())
                        .duracionMinutos(s.getDuracionMinutos())
                        .build())
                .collect(Collectors.toList());

        return CitaMultipleServiciosResponse.builder()
                .cita(mapToResponse(cita))
                .servicios(serviciosInfo)
                .precioTotal(precioTotal)
                .duracionTotal(duracionTotal)
                .build();
    }

    private CitaResponse mapToResponse(Cita cita) {
        return CitaResponse.fromEntity(cita);
    }
}
