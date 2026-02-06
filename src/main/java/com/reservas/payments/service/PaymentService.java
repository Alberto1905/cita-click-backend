package com.reservas.payments.service;

import com.reservas.entity.Cita;
import com.reservas.entity.Payment;
import com.reservas.entity.StripeConnectedAccount;
import com.reservas.exception.PaymentException;
import com.reservas.payments.domain.PaymentIntent;
import com.reservas.payments.domain.Refund;
import com.reservas.payments.dto.CreatePaymentRequest;
import com.reservas.payments.dto.RefundRequest;
import com.reservas.payments.stripe.StripePaymentProvider;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.PaymentRepository;
import com.reservas.repository.StripeConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de negocio para gestionar pagos a través de Stripe Connect.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final StripePaymentProvider paymentProvider;
    private final PaymentRepository paymentRepository;
    private final StripeConnectedAccountRepository accountRepository;
    private final CitaRepository citaRepository;

    /**
     * Crea un PaymentIntent para cobrar a un cliente final.
     */
    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        log.info("Creando pago - Usuario: {}, Monto: {} {}",
                request.getUsuarioId(), request.getAmount(), request.getCurrency());

        StripeConnectedAccount account = accountRepository.findByUsuarioId(UUID.fromString(request.getUsuarioId()))
                .orElseThrow(() -> new PaymentException(
                        "El usuario no tiene una cuenta conectada",
                        "NO_CONNECTED_ACCOUNT"
                ));

        if (!account.isReadyForPayments()) {
            throw new PaymentException(
                    "La cuenta no está habilitada para recibir pagos. Complete el onboarding.",
                    "ACCOUNT_NOT_READY"
            );
        }

        String idempotencyKey = UUID.randomUUID().toString();

        PaymentIntent paymentIntent = paymentProvider.createPaymentIntentForConnectedAccount(
                request,
                account.getStripeAccountId(),
                idempotencyKey
        );

        BigDecimal platformFee = paymentIntent.getPlatformFee();
        BigDecimal netAmount = request.getAmount().subtract(platformFee);

        Payment payment = Payment.builder()
                .usuario(account.getUsuario())
                .stripeAccount(account)
                .paymentIntentId(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .platformFee(platformFee)
                .platformFeePercentage(request.getPlatformFeePercentage())
                .netAmount(netAmount)
                .status(Payment.PaymentStatus.PENDING)
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .description(request.getDescription())
                .idempotencyKey(idempotencyKey)
                .refunded(false)
                .build();

        if (request.getCitaId() != null) {
            Cita cita = citaRepository.findById(request.getCitaId())
                    .orElseThrow(() -> new PaymentException("Cita no encontrada", "CITA_NOT_FOUND"));
            payment.setCita(cita);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Pago creado exitosamente: {} - PaymentIntent: {}",
                saved.getId(), saved.getPaymentIntentId());

        return saved;
    }

    /**
     * Maneja el éxito de un pago (llamado desde webhook).
     */
    @Transactional
    public void handlePaymentSuccess(String paymentIntentId, String chargeId) {
        log.info("Procesando pago exitoso: {}", paymentIntentId);

        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentException("Pago no encontrado", "PAYMENT_NOT_FOUND"));

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setChargeId(chargeId);
        payment.setConfirmedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        if (payment.getCita() != null) {
            Cita cita = payment.getCita();
            cita.setEstado(Cita.EstadoCita.CONFIRMADA);
            citaRepository.save(cita);
            log.info("Cita confirmada automáticamente: {}", cita.getId());
        }

        log.info("Pago procesado exitosamente: {} - Amount: {} {}",
                payment.getId(), payment.getAmount(), payment.getCurrency());
    }

    /**
     * Maneja el fallo de un pago (llamado desde webhook).
     */
    @Transactional
    public void handlePaymentFailed(String paymentIntentId) {
        log.warn("Procesando pago fallido: {}", paymentIntentId);

        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentException("Pago no encontrado", "PAYMENT_NOT_FOUND"));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        paymentRepository.save(payment);

        log.warn("Pago marcado como fallido: {}", payment.getId());
    }

    /**
     * Crea un reembolso para un pago.
     */
    @Transactional
    public Refund createRefund(RefundRequest request) {
        log.info("Creando reembolso para PaymentIntent: {}", request.getPaymentIntentId());

        Payment payment = paymentRepository.findByPaymentIntentId(request.getPaymentIntentId())
                .orElseThrow(() -> new PaymentException("Pago no encontrado", "PAYMENT_NOT_FOUND"));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS && payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
            throw new PaymentException("Solo se pueden reembolsar pagos exitosos", "INVALID_PAYMENT_STATUS");
        }

        Refund refund = paymentProvider.createRefund(request);

        payment.setRefunded(true);
        payment.setAmountRefunded(request.getAmount() != null ? request.getAmount() : payment.getAmount());
        payment.setRefundId(refund.getId());
        payment.setRefundAmount(refund.getAmount());
        payment.setRefundReason(refund.getReason());
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("Reembolso creado exitosamente: {}", refund.getId());
        return refund;
    }

    /**
     * Crea un reembolso para un pago verificando autorización del usuario.
     */
    @Transactional
    public Payment createRefund(RefundRequest request, String email) {
        log.info("Creando reembolso para pago - Usuario: {}", email);

        if (request.getPaymentId() == null || request.getPaymentId().isEmpty()) {
            throw new PaymentException("Payment ID es requerido", "PAYMENT_ID_REQUIRED");
        }

        Payment payment = getPaymentById(request.getPaymentId());

        if (!payment.getUsuario().getEmail().equals(email)) {
            throw new PaymentException("No tiene permiso para reembolsar este pago", "UNAUTHORIZED");
        }

        // Crear request con paymentIntentId
        RefundRequest stripeRequest = RefundRequest.builder()
                .paymentIntentId(payment.getPaymentIntentId())
                .amount(request.getAmount())
                .reason(request.getReason())
                .build();

        createRefund(stripeRequest);

        // Retornar el pago actualizado
        return getPaymentById(payment.getId());
    }

    /**
     * Obtiene los pagos de un usuario.
     */
    public Page<Payment> getPaymentsByUsuario(String usuarioId, Pageable pageable) {
        return paymentRepository.findByUsuarioId(UUID.fromString(usuarioId), pageable);
    }

    /**
     * Obtiene un pago por su ID.
     */
    public Payment getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Pago no encontrado", "PAYMENT_NOT_FOUND"));
    }

    /**
     * Encuentra pagos por email del usuario.
     */
    public Page<Payment> findByUsuarioEmail(String email, Pageable pageable) {
        return paymentRepository.findByUsuarioEmail(email, pageable);
    }

    /**
     * Encuentra pagos por email del usuario y estado.
     */
    public Page<Payment> findByUsuarioEmailAndStatus(String email, String status, Pageable pageable) {
        try {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            return paymentRepository.findByUsuarioEmailAndStatus(email, paymentStatus, pageable);
        } catch (IllegalArgumentException e) {
            throw new PaymentException("Estado de pago inválido: " + status, "INVALID_STATUS");
        }
    }

    /**
     * Encuentra un pago por ID y verifica que pertenezca al usuario.
     */
    public Payment findById(String paymentId, String email) {
        Payment payment = getPaymentById(paymentId);
        if (!payment.getUsuario().getEmail().equals(email)) {
            throw new PaymentException("No tiene permiso para acceder a este pago", "UNAUTHORIZED");
        }
        return payment;
    }

    /**
     * Obtiene estadísticas de pagos del usuario.
     */
    public Map<String, Object> getPaymentStatistics(String email) {
        Page<Payment> allPayments = findByUsuarioEmail(email, Pageable.unpaged());

        long totalPayments = allPayments.getTotalElements();
        BigDecimal totalAmount = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCEEDED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformFees = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCEEDED)
                .map(Payment::getPlatformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetAmount = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCEEDED)
                .map(Payment::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successfulPayments = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCEEDED)
                .count();

        long failedPayments = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.FAILED)
                .count();

        long refundedPayments = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.REFUNDED)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPayments", totalPayments);
        stats.put("successfulPayments", successfulPayments);
        stats.put("failedPayments", failedPayments);
        stats.put("refundedPayments", refundedPayments);
        stats.put("totalAmount", totalAmount);
        stats.put("totalPlatformFees", totalPlatformFees);
        stats.put("totalNetAmount", totalNetAmount);
        stats.put("currency", allPayments.getContent().isEmpty() ? "MXN" : allPayments.getContent().get(0).getCurrency());

        return stats;
    }
}
