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

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new PaymentException("Solo se pueden reembolsar pagos exitosos", "INVALID_PAYMENT_STATUS");
        }

        Refund refund = paymentProvider.createRefund(request);

        payment.setRefunded(true);
        payment.setAmountRefunded(request.getAmount() != null ? request.getAmount() : payment.getAmount());
        paymentRepository.save(payment);

        log.info("Reembolso creado exitosamente: {}", refund.getId());
        return refund;
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
}
