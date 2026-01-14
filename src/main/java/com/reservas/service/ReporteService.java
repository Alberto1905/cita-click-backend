package com.reservas.service;

import com.reservas.dto.response.ReporteResponse;
import com.reservas.entity.Cita;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.ClienteRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReporteService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public ReporteResponse generarReporteDiario(String email, LocalDate fecha) {
        log.info("Generando reporte diario para usuario: {} - Fecha: {}", email, fecha);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();

        return generarReporte(negocio, inicio, fin, "DIARIO");
    }

    @Transactional(readOnly = true)
    public ReporteResponse generarReporteSemanal(String email, LocalDate fechaInicio) {
        log.info("Generando reporte semanal para usuario: {} - Inicio: {}", email, fechaInicio);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaInicio.plusDays(7).atStartOfDay();

        return generarReporte(negocio, inicio, fin, "SEMANAL");
    }

    @Transactional(readOnly = true)
    public ReporteResponse generarReporteMensual(String email, int mes, int anio) {
        log.info("Generando reporte mensual para usuario: {} - Mes: {}/{}", email, mes, anio);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        LocalDate fechaInicio = LocalDate.of(anio, mes, 1);
        LocalDate fechaFin = fechaInicio.plusMonths(1);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atStartOfDay();

        return generarReporte(negocio, inicio, fin, "MENSUAL");
    }

    private ReporteResponse generarReporte(Negocio negocio, LocalDateTime inicio, LocalDateTime fin, String periodo) {
        // Obtener citas del periodo
        List<Cita> citas = citaRepository.findByNegocioAndFechaHoraBetween(negocio, inicio, fin);

        // Calcular estadísticas por estado
        Map<Cita.EstadoCita, Long> citasPorEstado = citas.stream()
                .collect(Collectors.groupingBy(Cita::getEstado, Collectors.counting()));

        int totalCitas = citas.size();
        int citasPendientes = citasPorEstado.getOrDefault(Cita.EstadoCita.PENDIENTE, 0L).intValue();
        int citasConfirmadas = citasPorEstado.getOrDefault(Cita.EstadoCita.CONFIRMADA, 0L).intValue();
        int citasCompletadas = citasPorEstado.getOrDefault(Cita.EstadoCita.COMPLETADA, 0L).intValue();
        int citasCanceladas = citasPorEstado.getOrDefault(Cita.EstadoCita.CANCELADA, 0L).intValue();

        // Calcular ingresos
        BigDecimal ingresoTotal = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .map(c -> c.getServicio().getPrecio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ingresoEstimado = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.PENDIENTE || c.getEstado() == Cita.EstadoCita.CONFIRMADA)
                .map(c -> c.getServicio().getPrecio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Clientes
        int clientesTotales = clienteRepository.findByNegocio(negocio).size();
        int clientesNuevos = (int) clienteRepository.findByNegocio(negocio).stream()
                .filter(c -> c.getCreatedAt().isAfter(inicio) && c.getCreatedAt().isBefore(fin))
                .count();

        // Servicio más popular
        Map<String, Long> serviciosPorCantidad = citas.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getServicio().getNombre(),
                        Collectors.counting()
                ));

        String servicioMasPopular = null;
        Integer servicioMasPopularCantidad = 0;

        if (!serviciosPorCantidad.isEmpty()) {
            Map.Entry<String, Long> servicioTop = serviciosPorCantidad.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (servicioTop != null) {
                servicioMasPopular = servicioTop.getKey();
                servicioMasPopularCantidad = servicioTop.getValue().intValue();
            }
        }

        log.info(" Reporte {} generado: {} citas, ${} ingresos", periodo, totalCitas, ingresoTotal);

        return ReporteResponse.builder()
                .fechaInicio(inicio.toLocalDate())
                .fechaFin(fin.toLocalDate())
                .periodo(periodo)
                .totalCitas(totalCitas)
                .citasPendientes(citasPendientes)
                .citasConfirmadas(citasConfirmadas)
                .citasCompletadas(citasCompletadas)
                .citasCanceladas(citasCanceladas)
                .ingresoTotal(ingresoTotal)
                .ingresoEstimado(ingresoEstimado)
                .clientesNuevos(clientesNuevos)
                .clientesTotales(clientesTotales)
                .servicioMasPopular(servicioMasPopular)
                .servicioMasPopularCantidad(servicioMasPopularCantidad)
                .build();
    }
}
