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

            // Crear metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("negocio_id", negocio.getId().toString());
            metadata.put("plan", request.getPlan());
            metadata.put("email", emailUsuario);
            if (request.getReferencia() != null) {
                metadata.put("referencia", request.getReferencia());
            }

            // Crear sesión de checkout
            SessionCreateParams params = SessionCreateParams.builder()
                    .setUiMode(SessionCreateParams.UiMode.EMBEDDED) // Checkout embebido
                    .setMode(SessionCreateParams.Mode.PAYMENT) // Pago único
                    .setCustomer(customerId)
                    .setReturnUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .putAllMetadata(metadata)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setAutomaticTax(
                            SessionCreateParams.AutomaticTax.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

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
                    .clientSecret(session.getClientSecret())
                    .url(session.getUrl())
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
     * Obtiene el estado de una sesión de checkout
     */
    public Map<String, Object> obtenerEstadoSesion(String sessionId) {
        log.info("[Stripe] Obteniendo estado de sesión: {}", sessionId);

        try {
            Session session = Session.retrieve(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", session.getStatus());
            response.put("payment_status", session.getPaymentStatus());

            if (session.getCustomerDetails() != null) {
                response.put("customer_email", session.getCustomerDetails().getEmail());
            }

            if (session.getPaymentIntent() != null) {
                response.put("payment_intent", session.getPaymentIntent());
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
}
