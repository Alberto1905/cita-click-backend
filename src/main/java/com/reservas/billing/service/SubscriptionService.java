package com.reservas.billing.service;

import com.reservas.billing.domain.Customer;
import com.reservas.billing.domain.Invoice;
import com.reservas.billing.domain.Subscription;
import com.reservas.billing.dto.CreateCustomerRequest;
import com.reservas.billing.dto.CreateSubscriptionRequest;
import com.reservas.billing.dto.UpdateSubscriptionRequest;
import com.reservas.billing.stripe.StripeBillingProvider;
import com.reservas.entity.StripeSubscription;
import com.reservas.entity.Usuario;
import com.reservas.exception.BillingException;
import com.reservas.repository.StripeSubscriptionRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Servicio de negocio para gestionar suscripciones del SaaS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final StripeBillingProvider billingProvider;
    private final StripeSubscriptionRepository subscriptionRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea una suscripción para un usuario.
     */
    @Transactional
    public StripeSubscription createSubscription(String usuarioId, CreateSubscriptionRequest request) {
        log.info("Creando suscripción para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(java.util.UUID.fromString(usuarioId))
                .orElseThrow(() -> new BillingException("Usuario no encontrado", "USER_NOT_FOUND"));

        String customerId = request.getCustomerId();
        if (customerId == null) {
            CreateCustomerRequest customerRequest = CreateCustomerRequest.builder()
                    .usuarioId(usuarioId)
                    .email(usuario.getEmail())
                    .name(usuario.getNombre())
                    .build();

            Customer customer = billingProvider.createCustomer(customerRequest);
            customerId = customer.getId();
            log.info("Customer creado en Stripe: {}", customerId);
        }

        CreateSubscriptionRequest fullRequest = CreateSubscriptionRequest.builder()
                .customerId(customerId)
                .priceId(request.getPriceId())
                .paymentMethodId(request.getPaymentMethodId())
                .trialPeriodDays(request.getTrialPeriodDays())
                .trialEnd(request.getTrialEnd())
                .metadata(request.getMetadata())
                .build();

        Subscription subscription = billingProvider.createSubscription(fullRequest);

        StripeSubscription entity = mapToEntity(subscription, usuario);
        StripeSubscription saved = subscriptionRepository.save(entity);

        log.info("Suscripción creada exitosamente: {}", saved.getSubscriptionId());
        return saved;
    }

    /**
     * Actualiza una suscripción.
     */
    @Transactional
    public StripeSubscription updateSubscription(String subscriptionId, UpdateSubscriptionRequest request) {
        log.info("Actualizando suscripción: {}", subscriptionId);

        StripeSubscription entity = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BillingException("Suscripción no encontrada", "SUBSCRIPTION_NOT_FOUND"));

        Subscription updated = billingProvider.updateSubscription(subscriptionId, request);

        entity.setPriceId(updated.getPriceId());
        entity.setPlanName(updated.getPlanName());
        entity.setStatus(StripeSubscription.SubscriptionStatus.valueOf(updated.getStatus().name()));
        entity.setAmount(updated.getAmount());
        entity.setCancelAtPeriodEnd(updated.getCancelAtPeriodEnd());

        if (request.getPaymentMethodId() != null) {
            entity.setDefaultPaymentMethodId(request.getPaymentMethodId());
        }

        StripeSubscription saved = subscriptionRepository.save(entity);
        log.info("Suscripción actualizada: {}", saved.getSubscriptionId());

        return saved;
    }

    /**
     * Cancela una suscripción.
     */
    @Transactional
    public void cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd) {
        log.info("Cancelando suscripción: {} - At period end: {}", subscriptionId, cancelAtPeriodEnd);

        StripeSubscription entity = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new BillingException("Suscripción no encontrada", "SUBSCRIPTION_NOT_FOUND"));

        billingProvider.cancelSubscription(subscriptionId, cancelAtPeriodEnd);

        if (cancelAtPeriodEnd) {
            entity.setCancelAtPeriodEnd(true);
        } else {
            entity.setStatus(StripeSubscription.SubscriptionStatus.CANCELED);
            entity.setCanceledAt(LocalDateTime.now());
        }

        subscriptionRepository.save(entity);
        log.info("Suscripción cancelada: {}", subscriptionId);
    }

    /**
     * Maneja el pago exitoso de una factura (webhook).
     */
    @Transactional
    public void handleInvoicePaid(String invoiceId) {
        log.info("Procesando factura pagada: {}", invoiceId);

        Invoice invoice = billingProvider.getInvoice(invoiceId);

        if (invoice.getSubscriptionId() != null) {
            StripeSubscription entity = subscriptionRepository.findBySubscriptionId(invoice.getSubscriptionId())
                    .orElse(null);

            if (entity != null) {
                entity.setStatus(StripeSubscription.SubscriptionStatus.ACTIVE);
                entity.setLatestInvoiceId(invoiceId);
                subscriptionRepository.save(entity);

                log.info("Suscripción actualizada a ACTIVE: {}", entity.getSubscriptionId());
            }
        }
    }

    /**
     * Maneja el fallo de pago de una factura (webhook).
     */
    @Transactional
    public void handleInvoicePaymentFailed(String invoiceId) {
        log.warn("Procesando fallo de pago de factura: {}", invoiceId);

        Invoice invoice = billingProvider.getInvoice(invoiceId);

        if (invoice.getSubscriptionId() != null) {
            StripeSubscription entity = subscriptionRepository.findBySubscriptionId(invoice.getSubscriptionId())
                    .orElse(null);

            if (entity != null) {
                entity.setStatus(StripeSubscription.SubscriptionStatus.PAST_DUE);
                subscriptionRepository.save(entity);

                log.warn("Suscripción marcada como PAST_DUE: {}", entity.getSubscriptionId());
            }
        }
    }

    /**
     * Maneja la eliminación de una suscripción (webhook).
     */
    @Transactional
    public void handleSubscriptionDeleted(String subscriptionId) {
        log.info("Procesando eliminación de suscripción: {}", subscriptionId);

        StripeSubscription entity = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElse(null);

        if (entity != null) {
            entity.setStatus(StripeSubscription.SubscriptionStatus.CANCELED);
            entity.setEndedAt(LocalDateTime.now());
            subscriptionRepository.save(entity);

            log.info("Suscripción marcada como CANCELED: {}", subscriptionId);
        }
    }

    /**
     * Obtiene las suscripciones de un usuario.
     */
    public List<StripeSubscription> getSubscriptionsByUsuario(String usuarioId) {
        return subscriptionRepository.findByUsuarioId(java.util.UUID.fromString(usuarioId));
    }

    /**
     * Verifica si un usuario tiene una suscripción activa.
     */
    public boolean hasActiveSubscription(String usuarioId) {
        return subscriptionRepository.existsByUsuarioIdAndStatus(
                java.util.UUID.fromString(usuarioId),
                StripeSubscription.SubscriptionStatus.ACTIVE
        );
    }

    private StripeSubscription mapToEntity(Subscription subscription, Usuario usuario) {
        return StripeSubscription.builder()
                .usuario(usuario)
                .customerId(subscription.getCustomerId())
                .subscriptionId(subscription.getId())
                .priceId(subscription.getPriceId())
                .planName(subscription.getPlanName())
                .status(StripeSubscription.SubscriptionStatus.valueOf(subscription.getStatus().name()))
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .billingInterval(mapBillingInterval(subscription.getInterval()))
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .canceledAt(subscription.getCanceledAt())
                .trialPeriod(subscription.getTrialStart() != null)
                .trialStart(subscription.getTrialStart())
                .trialEnd(subscription.getTrialEnd())
                .defaultPaymentMethodId(subscription.getDefaultPaymentMethodId())
                .latestInvoiceId(subscription.getLatestInvoiceId())
                .build();
    }

    private StripeSubscription.BillingInterval mapBillingInterval(String interval) {
        if (interval == null) return null;
        return StripeSubscription.BillingInterval.valueOf(interval.toUpperCase());
    }
}
