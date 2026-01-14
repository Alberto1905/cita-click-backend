package com.reservas.service;

import com.reservas.dto.PlanLimitesDTO;
import com.reservas.dto.UsoNegocioDTO;
import com.reservas.entity.Negocio;
import com.reservas.entity.PlanLimites;
import com.reservas.entity.UsoNegocio;
import com.reservas.entity.Usuario;
import com.reservas.entity.enums.TipoPlan;
import com.reservas.exception.LimiteExcedidoException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanLimitesService {

    private final PlanLimitesRepository planLimitesRepository;
    private final UsoNegocioRepository usoNegocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;

    /**
     * Inicializa los límites de los planes en la base de datos
     * NOTA: La inicialización principal se hace vía migración V9__update_plan_limites.sql
     * Este método solo verifica que existan y los crea si faltan (fallback)
     */
    @PostConstruct
    @Transactional
    public void inicializarLimites() {
        log.info("[PlanLimitesService] Verificando límites de planes...");

        // PLAN BÁSICO
        if (!planLimitesRepository.existsByTipoPlan(TipoPlan.BASICO)) {
            PlanLimites basico = PlanLimites.builder()
                    .tipoPlan(TipoPlan.BASICO)
                    .maxUsuarios(2)
                    .maxClientes(50)
                    .maxCitasMes(100)
                    .maxServicios(10)
                    .smsWhatsappHabilitado(false)
                    .reportesAvanzadosHabilitado(false)
                    .soportePrioritario(false)
                    .build();
            planLimitesRepository.save(basico);
            log.info("[PlanLimitesService] Plan BÁSICO creado");
        }

        // PLAN PROFESIONAL
        if (!planLimitesRepository.existsByTipoPlan(TipoPlan.PROFESIONAL)) {
            PlanLimites profesional = PlanLimites.builder()
                    .tipoPlan(TipoPlan.PROFESIONAL)
                    .maxUsuarios(5)
                    .maxClientes(300)
                    .maxCitasMes(500)
                    .maxServicios(30)
                    .smsWhatsappHabilitado(false) // Disponible Q2 2026
                    .reportesAvanzadosHabilitado(true)
                    .soportePrioritario(false)
                    .build();
            planLimitesRepository.save(profesional);
            log.info("[PlanLimitesService] Plan PROFESIONAL creado");
        }

        // PLAN PREMIUM
        if (!planLimitesRepository.existsByTipoPlan(TipoPlan.PREMIUM)) {
            PlanLimites premium = PlanLimites.builder()
                    .tipoPlan(TipoPlan.PREMIUM)
                    .maxUsuarios(999999) // Ilimitado
                    .maxClientes(999999) // Ilimitado
                    .maxCitasMes(999999) // Ilimitado
                    .maxServicios(999999) // Ilimitado
                    .smsWhatsappHabilitado(false) // Disponible Q2 2026
                    .reportesAvanzadosHabilitado(true)
                    .soportePrioritario(true)
                    .build();
            planLimitesRepository.save(premium);
            log.info("[PlanLimitesService] Plan PREMIUM creado");
        }

        log.info("[PlanLimitesService] Verificación de límites completada");
    }

    /**
     * Obtiene los límites de un plan específico
     */
    public PlanLimites obtenerLimites(TipoPlan tipoPlan) {
        return planLimitesRepository.findByTipoPlan(tipoPlan)
                .orElseThrow(() -> new IllegalStateException("Límites no encontrados para plan: " + tipoPlan));
    }

    /**
     * Obtiene el uso actual de un negocio
     */
    @Transactional
    public UsoNegocio obtenerUsoActual(UUID negocioId) {
        String periodoActual = UsoNegocio.getPeriodoActual();

        return usoNegocioRepository.findByNegocioIdAndPeriodo(negocioId, periodoActual)
                .orElseGet(() -> {
                    log.info("[PlanLimitesService] Creando nuevo registro de uso para negocio: {}, periodo: {}",
                            negocioId, periodoActual);
                    // Crear nuevo registro de uso
                    Negocio negocio = new Negocio();
                    negocio.setId(negocioId);

                    UsoNegocio nuevoUso = UsoNegocio.builder()
                            .negocio(negocio)
                            .periodo(periodoActual)
                            .totalUsuarios(0)
                            .totalClientes(0)
                            .totalCitasMes(0)
                            .totalServicios(0)
                            .build();

                    return usoNegocioRepository.save(nuevoUso);
                });
    }

    /**
     * Actualiza el conteo de uso de un negocio
     */
    @Transactional
    public void actualizarUso(UUID negocioId) {
        log.info("[PlanLimitesService] Actualizando uso para negocio: {}", negocioId);

        UsoNegocio uso = obtenerUsoActual(negocioId);

        // Contar totales reales
        long totalUsuarios = usuarioRepository.countActiveUsuariosByNegocioId(negocioId);
        long totalClientes = clienteRepository.countByNegocioId(negocioId);
        long totalServicios = servicioRepository.countByNegocioId(negocioId);

        // Para citas del mes actual
        String periodoActual = UsoNegocio.getPeriodoActual();
        String[] parts = periodoActual.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        long totalCitasMes = citaRepository.countCitasByNegocioAndMonth(negocioId, year, month);

        uso.setTotalUsuarios((int) totalUsuarios);
        uso.setTotalClientes((int) totalClientes);
        uso.setTotalCitasMes((int) totalCitasMes);
        uso.setTotalServicios((int) totalServicios);

        usoNegocioRepository.save(uso);
        log.info("[PlanLimitesService] Uso actualizado - Usuarios: {}, Clientes: {}, Citas: {}, Servicios: {}",
                totalUsuarios, totalClientes, totalCitasMes, totalServicios);
    }

    /**
     * Valida si se puede agregar un nuevo usuario
     */
    public void validarLimiteUsuarios(UUID negocioId, TipoPlan tipoPlan) {
        log.info("[PlanLimitesService] Validando límite de usuarios para negocio: {}, plan: {}", negocioId, tipoPlan);

        PlanLimites limites = obtenerLimites(tipoPlan);

        // Si es ilimitado, permitir
        if (limites.getMaxUsuarios() == -1) {
            log.info("[PlanLimitesService] Plan con usuarios ilimitados");
            return;
        }

        long usuariosActuales = usuarioRepository.countActiveUsuariosByNegocioId(negocioId);

        if (usuariosActuales >= limites.getMaxUsuarios()) {
            log.warn("[PlanLimitesService] Límite de usuarios excedido: {} >= {}", usuariosActuales, limites.getMaxUsuarios());
            throw new LimiteExcedidoException("usuarios", (int) usuariosActuales, limites.getMaxUsuarios());
        }

        log.info("[PlanLimitesService] Validación exitosa: {} / {} usuarios", usuariosActuales, limites.getMaxUsuarios());
    }

    /**
     * Valida si se puede agregar un nuevo cliente
     */
    public void validarLimiteClientes(UUID negocioId, TipoPlan tipoPlan) {
        log.info("[PlanLimitesService] Validando límite de clientes para negocio: {}, plan: {}", negocioId, tipoPlan);

        PlanLimites limites = obtenerLimites(tipoPlan);

        if (limites.getMaxClientes() == -1) {
            log.info("[PlanLimitesService] Plan con clientes ilimitados");
            return;
        }

        long clientesActuales = clienteRepository.countByNegocioId(negocioId);

        if (clientesActuales >= limites.getMaxClientes()) {
            log.warn("[PlanLimitesService] Límite de clientes excedido: {} >= {}", clientesActuales, limites.getMaxClientes());
            throw new LimiteExcedidoException("clientes", (int) clientesActuales, limites.getMaxClientes());
        }

        log.info("[PlanLimitesService] Validación exitosa: {} / {} clientes", clientesActuales, limites.getMaxClientes());
    }

    /**
     * Valida si se puede agregar una nueva cita este mes
     */
    public void validarLimiteCitasMes(UUID negocioId, TipoPlan tipoPlan) {
        log.info("[PlanLimitesService] Validando límite de citas del mes para negocio: {}, plan: {}", negocioId, tipoPlan);

        PlanLimites limites = obtenerLimites(tipoPlan);

        if (limites.getMaxCitasMes() == -1) {
            log.info("[PlanLimitesService] Plan con citas ilimitadas");
            return;
        }

        UsoNegocio uso = obtenerUsoActual(negocioId);

        if (uso.getTotalCitasMes() >= limites.getMaxCitasMes()) {
            log.warn("[PlanLimitesService] Límite de citas del mes excedido: {} >= {}",
                    uso.getTotalCitasMes(), limites.getMaxCitasMes());
            throw new LimiteExcedidoException("citas este mes", uso.getTotalCitasMes(), limites.getMaxCitasMes());
        }

        log.info("[PlanLimitesService] Validación exitosa: {} / {} citas este mes",
                uso.getTotalCitasMes(), limites.getMaxCitasMes());
    }

    /**
     * Valida si se puede agregar un nuevo servicio
     */
    public void validarLimiteServicios(UUID negocioId, TipoPlan tipoPlan) {
        log.info("[PlanLimitesService] Validando límite de servicios para negocio: {}, plan: {}", negocioId, tipoPlan);

        PlanLimites limites = obtenerLimites(tipoPlan);

        if (limites.getMaxServicios() == -1) {
            log.info("[PlanLimitesService] Plan con servicios ilimitados");
            return;
        }

        long serviciosActuales = servicioRepository.countByNegocioId(negocioId);

        if (serviciosActuales >= limites.getMaxServicios()) {
            log.warn("[PlanLimitesService] Límite de servicios excedido: {} >= {}",
                    serviciosActuales, limites.getMaxServicios());
            throw new LimiteExcedidoException("servicios", (int) serviciosActuales, limites.getMaxServicios());
        }

        log.info("[PlanLimitesService] Validación exitosa: {} / {} servicios",
                serviciosActuales, limites.getMaxServicios());
    }

    /**
     * Valida si una funcionalidad está habilitada en el plan
     */
    public void validarFuncionalidadHabilitada(TipoPlan tipoPlan, String funcionalidad) {
        log.info("[PlanLimitesService] Validando funcionalidad '{}' para plan: {}", funcionalidad, tipoPlan);

        PlanLimites limites = obtenerLimites(tipoPlan);

        boolean habilitada = switch (funcionalidad.toLowerCase()) {
            case "sms", "whatsapp", "sms_whatsapp" -> limites.isSmsWhatsappHabilitado();
            case "reportes_avanzados" -> limites.isReportesAvanzadosHabilitado();
            case "soporte_prioritario" -> limites.isSoportePrioritario();
            default -> throw new IllegalArgumentException("Funcionalidad no reconocida: " + funcionalidad);
        };

        if (!habilitada) {
            log.warn("[PlanLimitesService] Funcionalidad '{}' no habilitada para plan {}", funcionalidad, tipoPlan);
            throw new LimiteExcedidoException(
                    String.format("La funcionalidad '%s' no está disponible en el plan %s",
                            funcionalidad, tipoPlan.getNombre()));
        }

        log.info("[PlanLimitesService] Funcionalidad '{}' está habilitada", funcionalidad);
    }

    /**
     * Obtiene los límites del plan del negocio por email del usuario.
     *
     * SOLUCIÓN LAZY LOADING:
     * - Marcado como @Transactional(readOnly = true)
     * - Usa JOIN FETCH para cargar Negocio eagerly
     * - Previene LazyInitializationException
     */
    @Transactional(readOnly = true)
    public PlanLimitesDTO obtenerLimitesPorEmail(String email) {
        log.info("[PlanLimitesService] Obteniendo límites del plan para usuario: {}", email);

        // JOIN FETCH previene LazyInitializationException
        Usuario usuario = usuarioRepository.findByEmailWithNegocio(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new ResourceNotFoundException("Negocio no encontrado para el usuario");
        }

        TipoPlan plan = TipoPlan.fromCodigo(negocio.getPlan());
        PlanLimites limites = obtenerLimites(plan);

        return PlanLimitesDTO.builder()
                .tipoPlan(limites.getTipoPlan().getCodigo())
                .nombrePlan(limites.getTipoPlan().getNombre())
                .maxUsuarios(limites.getMaxUsuarios())
                .maxClientes(limites.getMaxClientes())
                .maxCitasMes(limites.getMaxCitasMes())
                .maxServicios(limites.getMaxServicios())
                .smsWhatsappHabilitado(limites.isSmsWhatsappHabilitado())
                .reportesAvanzadosHabilitado(limites.isReportesAvanzadosHabilitado())
                .soportePrioritario(limites.isSoportePrioritario())
                .build();
    }

    /**
     * Obtiene el uso actual del negocio por email del usuario.
     *
     * SOLUCIÓN LAZY LOADING:
     * - Marcado como @Transactional(readOnly = true)
     * - Usa JOIN FETCH para cargar Negocio eagerly
     * - Previene LazyInitializationException
     */
    @Transactional(readOnly = true)
    public UsoNegocioDTO obtenerUsoPorEmail(String email) {
        log.info("[PlanLimitesService] Obteniendo uso del plan para usuario: {}", email);

        // JOIN FETCH previene LazyInitializationException
        Usuario usuario = usuarioRepository.findByEmailWithNegocio(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new ResourceNotFoundException("Negocio no encontrado para el usuario");
        }

        UUID negocioId = negocio.getId();
        TipoPlan plan = TipoPlan.fromCodigo(negocio.getPlan());

        // Actualizar uso antes de obtenerlo
        actualizarUso(negocioId);

        UsoNegocio uso = obtenerUsoActual(negocioId);
        PlanLimites limites = obtenerLimites(plan);

        // Calcular porcentajes
        Double porcentajeUsuarios = UsoNegocioDTO.calcularPorcentaje(uso.getTotalUsuarios(), limites.getMaxUsuarios());
        Double porcentajeClientes = UsoNegocioDTO.calcularPorcentaje(uso.getTotalClientes(), limites.getMaxClientes());
        Double porcentajeCitasMes = UsoNegocioDTO.calcularPorcentaje(uso.getTotalCitasMes(), limites.getMaxCitasMes());
        Double porcentajeServicios = UsoNegocioDTO.calcularPorcentaje(uso.getTotalServicios(), limites.getMaxServicios());

        return UsoNegocioDTO.builder()
                .periodo(uso.getPeriodo())
                .totalUsuarios(uso.getTotalUsuarios())
                .totalClientes(uso.getTotalClientes())
                .totalCitasMes(uso.getTotalCitasMes())
                .totalServicios(uso.getTotalServicios())
                .limiteUsuarios(limites.getMaxUsuarios())
                .limiteClientes(limites.getMaxClientes())
                .limiteCitasMes(limites.getMaxCitasMes())
                .limiteServicios(limites.getMaxServicios())
                .porcentajeUsuarios(porcentajeUsuarios)
                .porcentajeClientes(porcentajeClientes)
                .porcentajeCitasMes(porcentajeCitasMes)
                .porcentajeServicios(porcentajeServicios)
                .alertaUsuarios(UsoNegocioDTO.esAlerta(porcentajeUsuarios))
                .alertaClientes(UsoNegocioDTO.esAlerta(porcentajeClientes))
                .alertaCitasMes(UsoNegocioDTO.esAlerta(porcentajeCitasMes))
                .alertaServicios(UsoNegocioDTO.esAlerta(porcentajeServicios))
                .build();
    }

    /**
     * Valida si una funcionalidad está habilitada por email del usuario.
     * Retorna true/false en lugar de lanzar excepción.
     *
     * SOLUCIÓN LAZY LOADING:
     * - Marcado como @Transactional(readOnly = true)
     * - Usa JOIN FETCH para cargar Negocio eagerly
     * - Previene LazyInitializationException
     */
    @Transactional(readOnly = true)
    public boolean validarFuncionalidadPorEmail(String email, String funcionalidad) {
        log.info("[PlanLimitesService] Validando funcionalidad '{}' para usuario: {}", funcionalidad, email);

        // JOIN FETCH previene LazyInitializationException
        Usuario usuario = usuarioRepository.findByEmailWithNegocio(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new ResourceNotFoundException("Negocio no encontrado para el usuario");
        }

        TipoPlan plan = TipoPlan.fromCodigo(negocio.getPlan());
        PlanLimites limites = obtenerLimites(plan);

        boolean habilitada = switch (funcionalidad.toLowerCase()) {
            case "sms", "whatsapp", "sms_whatsapp" -> limites.isSmsWhatsappHabilitado();
            case "reportes_avanzados" -> limites.isReportesAvanzadosHabilitado();
            case "soporte_prioritario" -> limites.isSoportePrioritario();
            default -> throw new IllegalArgumentException("Funcionalidad no reconocida: " + funcionalidad);
        };

        log.info("[PlanLimitesService] Funcionalidad '{}' está: {}", funcionalidad, habilitada ? "HABILITADA" : "DESHABILITADA");
        return habilitada;
    }
}
