package com.reservas.service;

import com.reservas.dto.response.ClientePerfil360Response;
import com.reservas.entity.Cita;
import com.reservas.entity.Cliente;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.ClienteRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para obtener el perfil 360 del cliente
 * Proporciona una vista completa del cliente incluyendo estadísticas e historial
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientePerfil360Service {

    private final ClienteRepository clienteRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene el perfil 360 completo de un cliente
     */
    @Transactional(readOnly = true)
    public ClientePerfil360Response obtenerPerfil360(String email, String clienteId) {
        log.info("[Perfil 360] Obteniendo perfil para cliente: {} por usuario: {}", clienteId, email);

        // Validar usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // Validar cliente
        Cliente cliente = clienteRepository.findById(UUID.fromString(clienteId))
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        if (!cliente.getNegocio().getId().equals(negocio.getId())) {
            throw new UnauthorizedException("El cliente no pertenece a tu negocio");
        }

        // Obtener todas las citas del cliente
        List<Cita> todasLasCitas = citaRepository.findByNegocio(negocio).stream()
                .filter(c -> c.getCliente().getId().equals(clienteId))
                .collect(Collectors.toList());

        log.info("[Perfil 360] Total de citas encontradas: {}", todasLasCitas.size());

        // Construir nombre completo
        String nombreCompleto = cliente.getNombre() + " " + cliente.getApellidoPaterno();
        if (cliente.getApellidoMaterno() != null && !cliente.getApellidoMaterno().isEmpty()) {
            nombreCompleto += " " + cliente.getApellidoMaterno();
        }

        // Construir response
        return ClientePerfil360Response.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .nombreCompleto(nombreCompleto)
                .email(cliente.getEmail())
                .telefono(cliente.getTelefono())
                .notas(cliente.getNotas())
                .fechaRegistro(cliente.getCreatedAt())
                .ultimaActualizacion(cliente.getUpdatedAt())
                .estadisticas(calcularEstadisticas(todasLasCitas))
                .historialCitas(construirHistorial(todasLasCitas))
                .proximasCitas(construirProximasCitas(todasLasCitas))
                .serviciosFrecuentes(calcularServiciosFrecuentes(todasLasCitas))
                .build();
    }

    /**
     * Calcula estadísticas generales de las citas del cliente
     */
    private ClientePerfil360Response.EstadisticasCitas calcularEstadisticas(List<Cita> citas) {
        Long totalCitas = (long) citas.size();

        Long citasCompletadas = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .count();

        Long citasCanceladas = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.CANCELADA)
                .count();

        Long citasPendientes = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.PENDIENTE)
                .count();

        Long citasConfirmadas = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.CONFIRMADA)
                .count();

        // Calcular gasto total (solo citas completadas)
        BigDecimal gastoTotal = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular gasto promedio
        BigDecimal gastoPromedio = citasCompletadas > 0
                ? gastoTotal.divide(BigDecimal.valueOf(citasCompletadas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Última cita (completada o confirmada en el pasado)
        LocalDateTime ultimaCita = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA ||
                        (c.getEstado() == Cita.EstadoCita.CONFIRMADA && c.getFechaHora().isBefore(LocalDateTime.now())))
                .map(Cita::getFechaHora)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // Próxima cita (confirmada o pendiente en el futuro)
        LocalDateTime proximaCita = citas.stream()
                .filter(c -> (c.getEstado() == Cita.EstadoCita.CONFIRMADA || c.getEstado() == Cita.EstadoCita.PENDIENTE)
                        && c.getFechaHora().isAfter(LocalDateTime.now()))
                .map(Cita::getFechaHora)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        return ClientePerfil360Response.EstadisticasCitas.builder()
                .totalCitas(totalCitas)
                .citasCompletadas(citasCompletadas)
                .citasCanceladas(citasCanceladas)
                .citasPendientes(citasPendientes)
                .citasConfirmadas(citasConfirmadas)
                .gastoTotal(gastoTotal)
                .gastoPromedio(gastoPromedio)
                .ultimaCita(ultimaCita)
                .proximaCita(proximaCita)
                .build();
    }

    /**
     * Construye el historial de citas (pasadas y completadas)
     */
    private List<ClientePerfil360Response.CitaResumen> construirHistorial(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getFechaHora().isBefore(LocalDateTime.now()) ||
                        c.getEstado() == Cita.EstadoCita.COMPLETADA ||
                        c.getEstado() == Cita.EstadoCita.CANCELADA)
                .sorted(Comparator.comparing(Cita::getFechaHora).reversed())
                .limit(50) // Últimas 50 citas
                .map(this::mapCitaToResumen)
                .collect(Collectors.toList());
    }

    /**
     * Construye la lista de próximas citas
     */
    private List<ClientePerfil360Response.CitaResumen> construirProximasCitas(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getFechaHora().isAfter(LocalDateTime.now()) &&
                        (c.getEstado() == Cita.EstadoCita.CONFIRMADA || c.getEstado() == Cita.EstadoCita.PENDIENTE))
                .sorted(Comparator.comparing(Cita::getFechaHora))
                .map(this::mapCitaToResumen)
                .collect(Collectors.toList());
    }

    /**
     * Calcula los servicios más frecuentes del cliente
     */
    private List<ClientePerfil360Response.ServicioUtilizado> calcularServiciosFrecuentes(List<Cita> citas) {
        // Agrupar por servicio
        Map<UUID, List<Cita>> citasPorServicio = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .collect(Collectors.groupingBy(c -> c.getServicio().getId()));

        return citasPorServicio.entrySet().stream()
                .map(entry -> {
                    UUID servicioId = entry.getKey();
                    List<Cita> citasServicio = entry.getValue();

                    String servicioNombre = citasServicio.get(0).getServicio().getNombre();
                    Long cantidadVeces = (long) citasServicio.size();

                    BigDecimal gastoTotal = citasServicio.stream()
                            .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    LocalDateTime ultimaVez = citasServicio.stream()
                            .map(Cita::getFechaHora)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    return ClientePerfil360Response.ServicioUtilizado.builder()
                            .servicioId(servicioId)
                            .servicioNombre(servicioNombre)
                            .cantidadVeces(cantidadVeces)
                            .gastoTotal(gastoTotal)
                            .ultimaVez(ultimaVez)
                            .build();
                })
                .sorted(Comparator.comparingLong(ClientePerfil360Response.ServicioUtilizado::getCantidadVeces).reversed())
                .limit(10) // Top 10 servicios
                .collect(Collectors.toList());
    }

    /**
     * Mapea una Cita a CitaResumen
     */
    private ClientePerfil360Response.CitaResumen mapCitaToResumen(Cita cita) {
        return ClientePerfil360Response.CitaResumen.builder()
                .id(cita.getId())
                .fechaHora(cita.getFechaHora())
                .fechaFin(cita.getFechaFin())
                .estado(cita.getEstado().name())
                .servicioNombre(cita.getServicio().getNombre())
                .precio(cita.getPrecio())
                .notas(cita.getNotas())
                .esRecurrente(cita.isEsRecurrente())
                .tipoRecurrencia(cita.getTipoRecurrencia() != null ? cita.getTipoRecurrencia().name() : null)
                .build();
    }
}
