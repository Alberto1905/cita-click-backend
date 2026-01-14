package com.reservas.service;

import com.reservas.dto.response.DashboardMetricasResponse;
import com.reservas.entity.Cita;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.ServicioRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para calcular métricas del dashboard
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardMetricasService {

    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;

    @Transactional(readOnly = true)
    public DashboardMetricasResponse obtenerMetricas(String email) {
        log.info("Calculando métricas del dashboard para usuario: {}", email);

        // Validar usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // Obtener todas las citas
        List<Cita> todasLasCitas = citaRepository.findByNegocio(negocio);

        // Calcular métricas
        DashboardMetricasResponse.IngresosMetricas ingresos = calcularIngresos(todasLasCitas);
        DashboardMetricasResponse.CitasMetricas citas = calcularCitas(todasLasCitas);
        DashboardMetricasResponse.ServiciosMetricas servicios = calcularServicios(todasLasCitas, negocio);
        List<DashboardMetricasResponse.TendenciaData> tendencia = calcularTendenciaSemanal(todasLasCitas);

        log.info("Métricas calculadas exitosamente");

        return DashboardMetricasResponse.builder()
                .ingresos(ingresos)
                .citas(citas)
                .servicios(servicios)
                .tendenciaSemanal(tendencia)
                .build();
    }

    private DashboardMetricasResponse.IngresosMetricas calcularIngresos(List<Cita> citas) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);
        LocalDate finMesAnterior = inicioMes.minusDays(1);

        // Citas completadas del mes actual
        List<Cita> citasMes = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(inicioMes))
                .collect(Collectors.toList());

        // Citas completadas de la semana actual
        List<Cita> citasSemana = citasMes.stream()
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(inicioSemana))
                .collect(Collectors.toList());

        // Citas completadas del mes anterior
        List<Cita> citasMesAnterior = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(inicioMesAnterior))
                .filter(c -> !c.getFechaHora().toLocalDate().isAfter(finMesAnterior))
                .collect(Collectors.toList());

        BigDecimal ingresoMensual = citasMes.stream()
                .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ingresoSemanal = citasSemana.stream()
                .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ingresoMesAnterior = citasMesAnterior.stream()
                .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular ingreso diario promedio (del mes actual)
        int diasTranscurridos = hoy.getDayOfMonth();
        BigDecimal ingresoDiarioPromedio = diasTranscurridos > 0
                ? ingresoMensual.divide(BigDecimal.valueOf(diasTranscurridos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calcular diferencia con mes anterior (porcentaje)
        BigDecimal diferenciaMesAnterior = BigDecimal.ZERO;
        if (ingresoMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            diferenciaMesAnterior = ingresoMensual.subtract(ingresoMesAnterior)
                    .divide(ingresoMesAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return DashboardMetricasResponse.IngresosMetricas.builder()
                .ingresoMensual(ingresoMensual)
                .ingresoSemanal(ingresoSemanal)
                .ingresoDiarioPromedio(ingresoDiarioPromedio)
                .diferenciaMesAnterior(diferenciaMesAnterior)
                .build();
    }

    private DashboardMetricasResponse.CitasMetricas calcularCitas(List<Cita> citas) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);

        // Total de citas del mes
        long totalMes = citas.stream()
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(inicioMes))
                .filter(c -> c.getEstado() != Cita.EstadoCita.CANCELADA)
                .count();

        // Total de citas de la semana
        long totalSemana = citas.stream()
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(inicioSemana))
                .filter(c -> c.getEstado() != Cita.EstadoCita.CANCELADA)
                .count();

        // Total de citas de hoy
        long totalHoy = citas.stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(hoy))
                .filter(c -> c.getEstado() != Cita.EstadoCita.CANCELADA)
                .count();

        // Citas por día de la semana (últimos 30 días)
        LocalDate hace30Dias = hoy.minusDays(30);
        Map<String, Long> citasPorDia = citas.stream()
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(hace30Dias))
                .filter(c -> c.getEstado() != Cita.EstadoCita.CANCELADA)
                .collect(Collectors.groupingBy(
                        c -> c.getFechaHora().getDayOfWeek()
                                .getDisplayName(TextStyle.FULL, new Locale("es", "ES")),
                        Collectors.counting()
                ));

        // Día con mayor demanda
        String diaMayorDemanda = citasPorDia.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // Citas por hora del día
        Map<String, Long> citasPorHora = citas.stream()
                .filter(c -> !c.getFechaHora().toLocalDate().isBefore(hace30Dias))
                .filter(c -> c.getEstado() != Cita.EstadoCita.CANCELADA)
                .collect(Collectors.groupingBy(
                        c -> String.format("%02d:00", c.getFechaHora().getHour()),
                        Collectors.counting()
                ));

        // Hora con mayor demanda
        String horaMayorDemanda = citasPorHora.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return DashboardMetricasResponse.CitasMetricas.builder()
                .totalMes(totalMes)
                .totalSemana(totalSemana)
                .totalHoy(totalHoy)
                .diaMayorDemanda(diaMayorDemanda)
                .horaMayorDemanda(horaMayorDemanda)
                .citasPorDia(citasPorDia)
                .citasPorHora(citasPorHora)
                .build();
    }

    private DashboardMetricasResponse.ServiciosMetricas calcularServicios(List<Cita> citas, Negocio negocio) {
        // Servicios activos
        int totalServiciosActivos = (int) servicioRepository.findByNegocio(negocio).stream()
                .filter(Servicio::isActivo)
                .count();

        // Agrupar citas completadas por servicio
        Map<String, List<Cita>> citasPorServicio = citas.stream()
                .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                .collect(Collectors.groupingBy(c -> c.getServicio().getId()));

        // Top 5 servicios más solicitados
        List<DashboardMetricasResponse.ServicioPopular> serviciosMasSolicitados = citasPorServicio.entrySet().stream()
                .map(entry -> {
                    String servicioId = entry.getKey();
                    List<Cita> citasServicio = entry.getValue();
                    String servicioNombre = citasServicio.get(0).getServicio().getNombre();
                    long cantidadCitas = citasServicio.size();
                    BigDecimal ingresoGenerado = citasServicio.stream()
                            .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return DashboardMetricasResponse.ServicioPopular.builder()
                            .id(servicioId)
                            .nombre(servicioNombre)
                            .cantidadCitas(cantidadCitas)
                            .ingresoGenerado(ingresoGenerado)
                            .build();
                })
                .sorted(Comparator.comparing(DashboardMetricasResponse.ServicioPopular::getCantidadCitas).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return DashboardMetricasResponse.ServiciosMetricas.builder()
                .serviciosMasSolicitados(serviciosMasSolicitados)
                .totalServiciosActivos(totalServiciosActivos)
                .build();
    }

    private List<DashboardMetricasResponse.TendenciaData> calcularTendenciaSemanal(List<Cita> citas) {
        LocalDate hoy = LocalDate.now();
        List<DashboardMetricasResponse.TendenciaData> tendencia = new ArrayList<>();

        // Últimos 7 días
        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            LocalDate finalFecha = fecha;

            long citasDia = citas.stream()
                    .filter(c -> c.getFechaHora().toLocalDate().equals(finalFecha))
                    .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                    .count();

            BigDecimal ingresosDia = citas.stream()
                    .filter(c -> c.getFechaHora().toLocalDate().equals(finalFecha))
                    .filter(c -> c.getEstado() == Cita.EstadoCita.COMPLETADA)
                    .map(c -> c.getPrecio() != null ? c.getPrecio() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            tendencia.add(DashboardMetricasResponse.TendenciaData.builder()
                    .fecha(fecha)
                    .citas(citasDia)
                    .ingresos(ingresosDia)
                    .build());
        }

        return tendencia;
    }
}
