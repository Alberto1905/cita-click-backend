package com.reservas.service;

import com.reservas.dto.request.CheckoutRequest;
import com.reservas.dto.response.CheckoutResponse;
import com.reservas.dto.response.PagoResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.Pago;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.PagoRepository;
import com.reservas.repository.UsuarioRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para integración con Stripe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final SuscripcionService suscripcionService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @Value("${stripe.price.basico}")
    private String priceIdBasico;

    @Value("${stripe.price.profesional}")
    private String priceIdProfesional;

    @Value("${stripe.price.premium}")
    private String priceIdPremium;

    // Precios en MXN (centavos)
    private static final Map<String, Long> PLAN_PRICES = Map.of(
            "basico", 29900L,        // $299.00 MXN
            "profesional", 69900L,   // $699.00 MXN
            "premium", 129900L       // $1,299.00 MXN
    );

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("[Stripe] Servicio inicializado con API Key");
    }

    /**
     * Crea una sesión de checkout para un plan
     */
    @Transactional
    public CheckoutResponse crearCheckoutSession(CheckoutRequest request, String emailUsuario) {
        log.info("[Stripe] Creando sesión de checkout para plan: {} - usuario: {}", request.getPlan(), emailUsuario);

        try {
            // Obtener usuario y negocio
            Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

            Negocio negocio = usuario.getNegocio();
            if (negocio == null) {
                throw new NotFoundException("Negocio no encontrado");
            }

            // Obtener o crear cliente de Stripe
            String customerId = obtenerOCrearCustomer(negocio);

            // Obtener el Price ID según el plan
            String priceId = getPriceIdForPlan(request.getPlan());

            // Verificar si el usuario ya usó su período de prueba
            boolean aplicarTrial = !usuario.isTrialUsed();

            // Crear metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("negocio_id", negocio.getId().toString());
            metadata.put("usuario_id", usuario.getId().toString());
            metadata.put("plan", request.getPlan());
            metadata.put("email", emailUsuario);
            metadata.put("trial_applied", String.valueOf(aplicarTrial));
            if (request.getReferencia() != null) {
                metadata.put("referencia", request.getReferencia());
            }

            // Crear sesión de checkout para SUSCRIPCIÓN (Hosted Checkout - redirect a Stripe)
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION) // Suscripción recurrente
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .putAllMetadata(metadata)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    // TODO: Reactivar cuando se configure dirección fiscal en Stripe Dashboard
                    // https://dashboard.stripe.com/test/settings/tax
                    .setAutomaticTax(
                            SessionCreateParams.AutomaticTax.builder()
                                    .setEnabled(false)
                                    .build()
                    );

            // Agregar trial solo si el usuario no lo ha usado antes
            if (aplicarTrial) {
                paramsBuilder.setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .setTrialPeriodDays(7L) // 7 días de prueba
                                .putMetadata("trial_granted", "true")
                                .build()
                );
                log.info("[Stripe] Aplicando período de prueba de 7 días para usuario: {}", emailUsuario);
            } else {
                log.info("[Stripe] Usuario ya usó su período de prueba, cobrando inmediatamente: {}", emailUsuario);
            }

            SessionCreateParams params = paramsBuilder.build();

            Session session = Session.create(params);

            // Guardar registro de pago pendiente
            Pago pago = Pago.builder()
                    .negocio(negocio)
                    .stripeCheckoutSessionId(session.getId())
                    .stripeCustomerId(customerId)
                    .plan(request.getPlan())
                    .monto(new BigDecimal(PLAN_PRICES.get(request.getPlan())).divide(new BigDecimal(100)))
                    .moneda("MXN")
                    .estado("pending")
                    .emailCliente(emailUsuario)
                    .descripcion("Suscripción mensual - Plan " + request.getPlan().toUpperCase())
                    .build();

            pagoRepository.save(pago);

            log.info("[Stripe]  Sesión de checkout creada: {}", session.getId());

            return CheckoutResponse.builder()
                    .sessionId(session.getId())
                    .clientSecret(null) // No se usa en Hosted Checkout
                    .url(session.getUrl()) // URL de Stripe Hosted Checkout
                    .plan(request.getPlan())
                    .monto(pago.getMonto().toString())
                    .moneda("MXN")
                    .build();

        } catch (StripeException e) {
            log.error("[Stripe] Error creando sesión de checkout", e);
            throw new RuntimeException("Error al crear sesión de pago: " + e.getMessage());
        }
    }

    /**
     * Obtiene el estado de una sesión de checkout.
     * Además, si la sesión está completa y es una suscripción, activa la suscripción
     * como respaldo del webhook (patrón recomendado por Stripe).
     */
    @Transactional
    public Map<String, Object> obtenerEstadoSesion(String sessionId) {
        log.info("[Stripe] Obteniendo estado de sesión: {}", sessionId);

        try {
            // Recuperar sesión con metadata expandida
            SessionRetrieveParams params = SessionRetrieveParams.builder()
                    .addExpand("line_items")
                    .build();
            Session session = Session.retrieve(sessionId, params, null);

            Map<String, Object> response = new HashMap<>();
            response.put("status", session.getStatus());
            response.put("payment_status", session.getPaymentStatus());

            if (session.getCustomerDetails() != null) {
                response.put("customer_email", session.getCustomerDetails().getEmail());
            }

            if (session.getPaymentIntent() != null) {
                response.put("payment_intent", session.getPaymentIntent());
            }

            // ============================================================
            // RESPALDO: Activar suscripción si la sesión está completa
            // Esto asegura la activación incluso si el webhook no llega
            // (ej: desarrollo local sin Stripe CLI)
            // ============================================================
            if ("complete".equals(session.getStatus()) && "subscription".equals(session.getMode())) {
                try {
                    String negocioId = session.getMetadata().get("negocio_id");
                    String plan = session.getMetadata().get("plan");

                    if (negocioId != null && plan != null) {
                        Negocio negocio = negocioRepository.findById(UUID.fromString(negocioId))
                                .orElse(null);

                        // Solo activar si aún no está activo con este plan
                        if (negocio != null && (!"activo".equals(negocio.getEstadoPago()) || !plan.equals(negocio.getPlan()))) {
                            log.info("[Stripe] Activando suscripción desde session-status (respaldo webhook)");
                            procesarSuscripcionCreada(session);
                        }
                    }
                } catch (Exception e) {
                    // No fallar la consulta de estado si la activación falla
                    log.warn("[Stripe] Error en activación de respaldo: {}", e.getMessage());
                }
            } else if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())
                    && session.getPaymentIntent() != null) {
                // Pago único completado
                try {
                    Pago pago = pagoRepository.findByStripeCheckoutSessionId(sessionId).orElse(null);
                    if (pago != null && !pago.isPagado()) {
                        log.info("[Stripe] Procesando pago desde session-status (respaldo webhook)");
                        procesarPagoCompletado(sessionId, session.getPaymentIntent());
                    }
                } catch (Exception e) {
                    log.warn("[Stripe] Error en procesamiento de pago de respaldo: {}", e.getMessage());
                }
            }

            return response;

        } catch (StripeException e) {
            log.error("[Stripe] Error obteniendo estado de sesión", e);
            throw new RuntimeException("Error al obtener estado de sesión: " + e.getMessage());
        }
    }

    /**
     * Procesa el webhook de Stripe cuando se completa un pago
     */
    @Transactional
    public void procesarPagoCompletado(String sessionId, String paymentIntentId) {
        log.info("[Stripe] Procesando pago completado - Session: {} - PaymentIntent: {}", sessionId, paymentIntentId);

        try {
            // Buscar el pago por session ID
            Pago pago = pagoRepository.findByStripeCheckoutSessionId(sessionId)
                    .orElseThrow(() -> new NotFoundException("Pago no encontrado para session: " + sessionId));

            // Si ya fue procesado, ignorar
            if (pago.isPagado()) {
                log.info("[Stripe] Pago ya procesado anteriormente: {}", pago.getId());
                return;
            }

            // Obtener detalles del PaymentIntent
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Actualizar el pago
            pago.setStripePaymentIntentId(paymentIntentId);
            pago.setEstado("completed");
            pago.setFechaCompletado(LocalDateTime.now());

            // Configurar período (30 días desde ahora)
            LocalDateTime ahora = LocalDateTime.now();
            pago.setPeriodoInicio(ahora);
            pago.setPeriodoFin(ahora.plusDays(30));

            // Método de pago
            if (paymentIntent.getPaymentMethod() != null) {
                pago.setMetodoPago(paymentIntent.getPaymentMethod());
            }

            // URL de factura - La obtendremos del PaymentIntent si está disponible
            // En versiones recientes de Stripe, se puede obtener desde la sesión o el customer
            if (paymentIntent.getReceiptEmail() != null) {
                log.info("[Stripe] Receipt email: {}", paymentIntent.getReceiptEmail());
            }

            pagoRepository.save(pago);

            // Activar la suscripción del negocio
            suscripcionService.activarSuscripcion(
                    pago.getNegocio().getId().toString(),
                    pago.getPlan()
            );

            log.info("[Stripe]  Pago procesado exitosamente: {} - Negocio: {}",
                    pago.getId(), pago.getNegocio().getNombre());

        } catch (StripeException e) {
            log.error("[Stripe] Error procesando pago completado", e);
            throw new RuntimeException("Error al procesar pago: " + e.getMessage());
        }
    }

    /**
     * Procesa el webhook cuando falla un pago
     */
    @Transactional
    public void procesarPagoFallido(String sessionId, String errorMessage) {
        log.warn("[Stripe] Procesando pago fallido - Session: {}", sessionId);

        Pago pago = pagoRepository.findByStripeCheckoutSessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));

        pago.setEstado("failed");
        pago.setErrorMensaje(errorMessage);
        pagoRepository.save(pago);

        log.info("[Stripe] Pago marcado como fallido: {}", pago.getId());
    }

    /**
     * Obtiene el historial de pagos de un negocio
     */
    @Transactional(readOnly = true)
    public List<PagoResponse> obtenerHistorialPagos(String emailUsuario) {
        log.info("[Stripe] Obteniendo historial de pagos para: {}", emailUsuario);

        // JOIN FETCH previene LazyInitializationException
        Usuario usuario = usuarioRepository.findByEmailWithNegocio(emailUsuario)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        List<Pago> pagos = pagoRepository.findByNegocioOrderByFechaCreacionDesc(usuario.getNegocio());

        return pagos.stream()
                .map(this::mapToPagoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de pagos del negocio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(String emailUsuario) {
        log.info("[Stripe] Obteniendo estadísticas de pagos para: {}", emailUsuario);

        Usuario usuario = usuarioRepository.findByEmailWithNegocio(emailUsuario)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado para el usuario");
        }

        long totalPagos = pagoRepository.countPagosCompletadosByNegocio(negocio);
        BigDecimal montoTotal = pagoRepository.sumMontoByNegocioAndEstadoCompleted(negocio);

        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }

        return Map.of(
                "totalPagos", totalPagos,
                "montoTotal", montoTotal
        );
    }

    // ==================== Métodos auxiliares ====================

    /**
     * Obtiene o crea un cliente de Stripe para el negocio
     */
    private String obtenerOCrearCustomer(Negocio negocio) throws StripeException {
        // Si ya tiene un Stripe Customer ID guardado, usarlo
        if (negocio.getStripeCustomerId() != null) {
            return negocio.getStripeCustomerId();
        }

        // Crear nuevo cliente en Stripe
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(negocio.getEmail())
                .setName(negocio.getNombre())
                .putMetadata("negocio_id", negocio.getId().toString())
                .build();

        Customer customer = Customer.create(params);

        // Guardar el Customer ID en el negocio
        negocio.setStripeCustomerId(customer.getId());
        negocioRepository.save(negocio);

        log.info("[Stripe]  Cliente creado en Stripe: {} para negocio: {}", customer.getId(), negocio.getNombre());

        return customer.getId();
    }

    /**
     * Obtiene el Price ID de Stripe para un plan
     */
    private String getPriceIdForPlan(String plan) {
        return switch (plan.toLowerCase()) {
            case "basico" -> priceIdBasico;
            case "profesional" -> priceIdProfesional;
            case "premium" -> priceIdPremium;
            default -> throw new IllegalArgumentException("Plan inválido: " + plan);
        };
    }

    /**
     * Mapea entidad Pago a DTO
     */
    private PagoResponse mapToPagoResponse(Pago pago) {
        return PagoResponse.builder()
                .id(pago.getId().toString())
                .plan(pago.getPlan())
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .estado(pago.getEstado())
                .metodoPago(pago.getMetodoPago())
                .periodoInicio(pago.getPeriodoInicio())
                .periodoFin(pago.getPeriodoFin())
                .descripcion(pago.getDescripcion())
                .facturaUrl(pago.getFacturaUrl())
                .fechaCreacion(pago.getFechaCreacion())
                .fechaCompletado(pago.getFechaCompletado())
                .errorMensaje(pago.getErrorMensaje())
                .build();
    }

    /**
     * Procesa la creación de una suscripción cuando se completa el checkout
     */
    @Transactional
    public void procesarSuscripcionCreada(com.stripe.model.checkout.Session session) {
        log.info("[Stripe] Procesando suscripción creada - Session: {}", session.getId());

        try {
            // Obtener metadata
            String usuarioId = session.getMetadata().get("usuario_id");
            String negocioId = session.getMetadata().get("negocio_id");
            String plan = session.getMetadata().get("plan");
            String trialApplied = session.getMetadata().get("trial_applied");

            if (usuarioId == null || negocioId == null) {
                log.error("[Stripe] Metadata incompleta en la sesión");
                return;
            }

            // Obtener usuario
            Usuario usuario = usuarioRepository.findById(UUID.fromString(usuarioId))
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

            // Marcar trial como usado si se aplicó
            if ("true".equals(trialApplied) && !usuario.isTrialUsed()) {
                usuario.setTrialUsed(true);
                usuario.setTrialEndsAt(LocalDateTime.now().plusDays(7));
                usuarioRepository.save(usuario);
                log.info("[Stripe] Trial marcado como usado para usuario: {}", usuario.getEmail());
            }

            // Obtener el negocio
            Negocio negocio = negocioRepository.findById(UUID.fromString(negocioId))
                    .orElseThrow(() -> new NotFoundException("Negocio no encontrado"));

            // Activar suscripción
            suscripcionService.activarSuscripcion(negocioId, plan);

            // Guardar subscription ID en el negocio
            negocio.setStripeSubscriptionId(session.getSubscription());
            negocioRepository.save(negocio);

            log.info("[Stripe] ✅ Suscripción activada para negocio: {} - Plan: {}", negocio.getNombre(), plan);

        } catch (Exception e) {
            log.error("[Stripe] Error procesando suscripción creada", e);
            throw new RuntimeException("Error al procesar suscripción: " + e.getMessage());
        }
    }
}
