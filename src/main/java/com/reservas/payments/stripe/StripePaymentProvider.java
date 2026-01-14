package com.reservas.payments.stripe;

import com.reservas.exception.PaymentException;
import com.reservas.payments.domain.PaymentIntent;
import com.reservas.payments.domain.PaymentResult;
import com.reservas.payments.domain.Refund;
import com.reservas.payments.dto.CreatePaymentRequest;
import com.reservas.payments.dto.RefundRequest;
import com.reservas.payments.provider.PaymentProvider;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent.PaymentMethodOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación de PaymentProvider usando Stripe.
 *
 * Esta clase gestiona los PaymentIntents para cobrar a clientes finales
 * a través de las cuentas conectadas (Stripe Connect).
 *
 * CARACTERÍSTICAS CLAVE:
 * - Usa application_fee_amount para comisión de la plataforma
 * - Los pagos van directamente a la cuenta conectada del usuario
 * - Soporte para idempotencia (evita duplicados)
 * - Validación de webhooks con signature
 */
@Slf4j
@Service
public class StripePaymentProvider implements PaymentProvider {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.platform.fee.percentage:5.0}")
    private BigDecimal defaultPlatformFeePercentage;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("[Stripe Payment] Inicializado - Comisión por defecto: {}%", defaultPlatformFeePercentage);
    }

    @Override
    public PaymentIntent createPaymentIntent(CreatePaymentRequest request) {
        try {
            log.info("[Stripe Payment] Creando PaymentIntent - Usuario: {}, Monto: {} {}",
                    request.getUsuarioId(), request.getAmount(), request.getCurrency());

            // Calcular comisión
            BigDecimal feePercentage = request.getPlatformFeePercentage() != null
                    ? request.getPlatformFeePercentage()
                    : defaultPlatformFeePercentage;

            Long platformFee = calculatePlatformFee(request.getAmount(), feePercentage);

            // Convertir monto a centavos (menor unidad)
            Long amountInCents = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            // Preparar metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("usuario_id", request.getUsuarioId());
            if (request.getCitaId() != null) {
                metadata.put("cita_id", request.getCitaId());
            }
            metadata.put("platform_fee_percentage", feePercentage.toString());
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }

            // CRÍTICO: Se necesita el stripe_account_id del usuario
            // Este debe obtenerse del servicio y pasarse aquí
            // Por ahora dejamos un placeholder que debe ser manejado en el servicio

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setApplicationFeeAmount(platformFee)
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (request.getCustomerEmail() != null) {
                paramsBuilder.setReceiptEmail(request.getCustomerEmail());
            }

            if (request.getDescription() != null) {
                paramsBuilder.setDescription(request.getDescription());
            }

            PaymentIntentCreateParams params = paramsBuilder.build();

            // IMPORTANTE: Este PaymentIntent se debe crear con el parámetro
            // de cuenta conectada usando RequestOptions
            // Esto se maneja en el servicio que llama a este método

            com.stripe.model.PaymentIntent stripePI = com.stripe.model.PaymentIntent.create(params);

            log.info("[Stripe Payment]  PaymentIntent creado: {} - Comisión: {} centavos ({}%)",
                    stripePI.getId(), platformFee, feePercentage);

            return mapPaymentIntentToDomain(stripePI);

        } catch (StripeException e) {
            log.error("[Stripe Payment]  Error creando PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al crear PaymentIntent: " + e.getUserMessage(),
                    e.getCode(),
                    e.getMessage()
            );
        }
    }

    /**
     * Crea un PaymentIntent en una cuenta conectada específica.
     * Este es el método principal que debe usarse para Stripe Connect.
     */
    public PaymentIntent createPaymentIntentForConnectedAccount(
            CreatePaymentRequest request,
            String connectedAccountId,
            String idempotencyKey
    ) {
        try {
            log.info("[Stripe Payment] Creando PaymentIntent para cuenta: {} - IdempKey: {}",
                    connectedAccountId, idempotencyKey);

            BigDecimal feePercentage = request.getPlatformFeePercentage() != null
                    ? request.getPlatformFeePercentage()
                    : defaultPlatformFeePercentage;

            Long platformFee = calculatePlatformFee(request.getAmount(), feePercentage);
            Long amountInCents = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("usuario_id", request.getUsuarioId());
            if (request.getCitaId() != null) {
                metadata.put("cita_id", request.getCitaId());
            }
            metadata.put("platform_fee_percentage", feePercentage.toString());
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setApplicationFeeAmount(platformFee)
                    .putAllMetadata(metadata)
                    .setDescription(request.getDescription())
                    .setReceiptEmail(request.getCustomerEmail())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            // Crear con RequestOptions para especificar la cuenta conectada
            com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                    .setStripeAccount(connectedAccountId)
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            com.stripe.model.PaymentIntent stripePI = com.stripe.model.PaymentIntent.create(params, requestOptions);

            log.info("[Stripe Payment]  PaymentIntent creado: {} - Cuenta: {} - Comisión: {}",
                    stripePI.getId(), connectedAccountId, platformFee);

            return mapPaymentIntentToDomain(stripePI);

        } catch (StripeException e) {
            log.error("[Stripe Payment]  Error creando PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al crear PaymentIntent: " + e.getUserMessage(),
                    e.getCode(),
                    e.getMessage()
            );
        }
    }

    @Override
    public PaymentResult confirmPayment(String paymentIntentId, String idempotencyKey) {
        try {
            log.info("[Stripe Payment] Confirmando PaymentIntent: {}", paymentIntentId);

            com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            com.stripe.model.PaymentIntent stripePI = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            stripePI = stripePI.confirm(PaymentIntentConfirmParams.builder().build(), requestOptions);

            boolean success = "succeeded".equals(stripePI.getStatus());

            log.info("[Stripe Payment] {} PaymentIntent confirmado: {} - Status: {}",
                    success ? "" : "", paymentIntentId, stripePI.getStatus());

            return PaymentResult.builder()
                    .success(success)
                    .paymentIntent(mapPaymentIntentToDomain(stripePI))
                    .chargeId(stripePI.getLatestCharge())
                    .message(success ? "Pago confirmado exitosamente" : "Pago requiere acción adicional")
                    .processedAt(LocalDateTime.now())
                    .build();

        } catch (StripeException e) {
            log.error("[Stripe Payment]  Error confirmando PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al confirmar pago: " + e.getUserMessage(),
                    e.getCode(),
                    e.getMessage()
            );
        }
    }

    @Override
    public PaymentIntent cancelPayment(String paymentIntentId) {
        try {
            log.info("[Stripe Payment] Cancelando PaymentIntent: {}", paymentIntentId);

            com.stripe.model.PaymentIntent stripePI = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            stripePI = stripePI.cancel(PaymentIntentCancelParams.builder().build());

            log.info("[Stripe Payment]  PaymentIntent cancelado: {}", paymentIntentId);

            return mapPaymentIntentToDomain(stripePI);

        } catch (StripeException e) {
            log.error("[Stripe Payment]  Error cancelando PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al cancelar pago: " + e.getUserMessage(),
                    e.getCode(),
                    e.getMessage()
            );
        }
    }

    @Override
    public Refund createRefund(RefundRequest request) {
        try {
            log.info("[Stripe Payment] Creando reembolso para: {}", request.getPaymentIntentId());

            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(request.getPaymentIntentId());

            if (request.getAmount() != null) {
                Long amountInCents = request.getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValue();
                paramsBuilder.setAmount(amountInCents);
            }

            if (request.getReason() != null) {
                paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
            }

            if (request.getMetadata() != null) {
                paramsBuilder.putAllMetadata(request.getMetadata());
            }

            com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.create(paramsBuilder.build());

            log.info("[Stripe Payment]  Reembolso creado: {}", stripeRefund.getId());

            return mapRefundToDomain(stripeRefund);

        } catch (StripeException e) {
            log.error("[Stripe Payment]  Error creando reembolso: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al crear reembolso: " + e.getUserMessage(),
                    e.getCode(),
                    e.getMessage()
            );
        }
    }

    @Override
    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        try {
            com.stripe.model.PaymentIntent stripePI = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            return mapPaymentIntentToDomain(stripePI);
        } catch (StripeException e) {
            log.error("[Stripe Payment]  Error obteniendo PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al obtener PaymentIntent: " + e.getUserMessage(),
                    e.getCode(),
                    e.getMessage()
            );
        }
    }

    @Override
    public Long calculatePlatformFee(BigDecimal amount, BigDecimal platformFeePercentage) {
        BigDecimal feeAmount = amount
                .multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Convertir a centavos
        return feeAmount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            Webhook.constructEvent(payload, signature, secret);
            return true;
        } catch (SignatureVerificationException e) {
            log.error("[Stripe Webhook]  Firma inválida: {}", e.getMessage());
            return false;
        }
    }

    private PaymentIntent mapPaymentIntentToDomain(com.stripe.model.PaymentIntent stripePI) {
        return PaymentIntent.builder()
                .id(stripePI.getId())
                .amount(BigDecimal.valueOf(stripePI.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .currency(stripePI.getCurrency())
                .status(PaymentIntent.PaymentIntentStatus.valueOf(stripePI.getStatus().toUpperCase()))
                .platformFee(stripePI.getApplicationFeeAmount() != null
                        ? BigDecimal.valueOf(stripePI.getApplicationFeeAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : null)
                .clientSecret(stripePI.getClientSecret())
                .description(stripePI.getDescription())
                .metadata(stripePI.getMetadata())
                .requiresAction("requires_action".equals(stripePI.getStatus()))
                .createdAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripePI.getCreated()), ZoneId.systemDefault()))
                .build();
    }

    private Refund mapRefundToDomain(com.stripe.model.Refund stripeRefund) {
        return Refund.builder()
                .id(stripeRefund.getId())
                .paymentIntentId(stripeRefund.getPaymentIntent())
                .chargeId(stripeRefund.getCharge())
                .amount(BigDecimal.valueOf(stripeRefund.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .currency(stripeRefund.getCurrency())
                .reason(stripeRefund.getReason())
                .status(Refund.RefundStatus.valueOf(stripeRefund.getStatus().toUpperCase()))
                .metadata(stripeRefund.getMetadata())
                .createdAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeRefund.getCreated()), ZoneId.systemDefault()))
                .build();
    }
}
