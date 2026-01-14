package com.reservas.billing.stripe;

import com.reservas.billing.domain.Customer;
import com.reservas.billing.domain.Invoice;
import com.reservas.billing.domain.Subscription;
import com.reservas.billing.dto.CreateCustomerRequest;
import com.reservas.billing.dto.CreateSubscriptionRequest;
import com.reservas.billing.dto.UpdateSubscriptionRequest;
import com.reservas.billing.provider.SubscriptionProvider;
import com.reservas.exception.BillingException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.*;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de Stripe Billing para suscripciones del SaaS.
 *
 * IMPORTANTE:
 * - Este provider gestiona los PAGOS DE LOS USUARIOS DEL SAAS (no clientes finales)
 * - Los pagos van a la cuenta PRINCIPAL de Stripe de la plataforma
 * - NO usar Stripe Connect aquí
 */
@Slf4j
@Service
public class StripeBillingProvider implements SubscriptionProvider {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("[Stripe Billing] Inicializado correctamente");
    }

    @Override
    public Customer createCustomer(CreateCustomerRequest request) {
        try {
            log.info("[Stripe Billing] Creando Customer para usuario: {}", request.getUsuarioId());

            Map<String, String> metadata = new HashMap<>();
            metadata.put("usuario_id", request.getUsuarioId());
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }

            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(request.getEmail())
                    .setName(request.getName())
                    .setPhone(request.getPhone())
                    .putAllMetadata(metadata)
                    .build();

            com.stripe.model.Customer stripeCustomer = com.stripe.model.Customer.create(params);

            log.info("[Stripe Billing]  Customer creado: {}", stripeCustomer.getId());

            return mapCustomerToDomain(stripeCustomer);

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error creando customer: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al crear customer: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Subscription createSubscription(CreateSubscriptionRequest request) {
        try {
            log.info("[Stripe Billing] Creando suscripción - Customer: {}, Price: {}",
                    request.getCustomerId(), request.getPriceId());

            SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(request.getCustomerId())
                    .addItem(
                            SubscriptionCreateParams.Item.builder()
                                    .setPrice(request.getPriceId())
                                    .build()
                    )
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE);

            if (request.getPaymentMethodId() != null) {
                paramsBuilder.setDefaultPaymentMethod(request.getPaymentMethodId());
            }

            if (request.getTrialPeriodDays() != null) {
                paramsBuilder.setTrialPeriodDays(Long.valueOf(request.getTrialPeriodDays()));
            } else if (request.getTrialEnd() != null) {
                paramsBuilder.setTrialEnd(
                        request.getTrialEnd().atZone(ZoneId.systemDefault()).toEpochSecond()
                );
            }

            if (request.getMetadata() != null) {
                paramsBuilder.putAllMetadata(request.getMetadata());
            }

            paramsBuilder.setPaymentSettings(
                    SubscriptionCreateParams.PaymentSettings.builder()
                            .setSaveDefaultPaymentMethod(
                                    SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION
                            )
                            .build()
            );

            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.create(paramsBuilder.build());

            log.info("[Stripe Billing]  Suscripción creada: {} - Status: {}",
                    stripeSub.getId(), stripeSub.getStatus());

            return mapSubscriptionToDomain(stripeSub);

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error creando suscripción: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al crear suscripción: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Subscription updateSubscription(String subscriptionId, UpdateSubscriptionRequest request) {
        try {
            log.info("[Stripe Billing] Actualizando suscripción: {}", subscriptionId);

            SubscriptionUpdateParams.Builder paramsBuilder = SubscriptionUpdateParams.builder();

            if (request.getPriceId() != null) {
                com.stripe.model.Subscription currentSub = com.stripe.model.Subscription.retrieve(subscriptionId);
                String itemId = currentSub.getItems().getData().get(0).getId();

                paramsBuilder.addItem(
                        SubscriptionUpdateParams.Item.builder()
                                .setId(itemId)
                                .setPrice(request.getPriceId())
                                .build()
                );
            }

            if (request.getPaymentMethodId() != null) {
                paramsBuilder.setDefaultPaymentMethod(request.getPaymentMethodId());
            }

            if (request.getCancelAtPeriodEnd() != null) {
                paramsBuilder.setCancelAtPeriodEnd(request.getCancelAtPeriodEnd());
            }

            if (request.getMetadata() != null) {
                paramsBuilder.putAllMetadata(request.getMetadata());
            }

            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);
            stripeSub = stripeSub.update(paramsBuilder.build());

            log.info("[Stripe Billing]  Suscripción actualizada: {}", subscriptionId);

            return mapSubscriptionToDomain(stripeSub);

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error actualizando suscripción: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al actualizar suscripción: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Subscription cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd) {
        try {
            log.info("[Stripe Billing] Cancelando suscripción: {} - At period end: {}",
                    subscriptionId, cancelAtPeriodEnd);

            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);

            if (cancelAtPeriodEnd) {
                stripeSub = stripeSub.update(
                        SubscriptionUpdateParams.builder()
                                .setCancelAtPeriodEnd(true)
                                .build()
                );
                log.info("[Stripe Billing]  Suscripción marcada para cancelar al final del periodo");
            } else {
                stripeSub = stripeSub.cancel();
                log.info("[Stripe Billing]  Suscripción cancelada inmediatamente");
            }

            return mapSubscriptionToDomain(stripeSub);

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error cancelando suscripción: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al cancelar suscripción: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Subscription reactivateSubscription(String subscriptionId) {
        try {
            log.info("[Stripe Billing] Reactivando suscripción: {}", subscriptionId);

            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);
            stripeSub = stripeSub.update(
                    SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(false)
                            .build()
            );

            log.info("[Stripe Billing]  Suscripción reactivada");

            return mapSubscriptionToDomain(stripeSub);

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error reactivando suscripción: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al reactivar suscripción: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Subscription getSubscription(String subscriptionId) {
        try {
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);
            return mapSubscriptionToDomain(stripeSub);
        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error obteniendo suscripción: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al obtener suscripción: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public List<Subscription> getCustomerSubscriptions(String customerId) {
        try {
            SubscriptionListParams params = SubscriptionListParams.builder()
                    .setCustomer(customerId)
                    .build();

            com.stripe.model.SubscriptionCollection subs = com.stripe.model.Subscription.list(params);

            return subs.getData().stream()
                    .map(this::mapSubscriptionToDomain)
                    .collect(Collectors.toList());

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error obteniendo suscripciones: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al obtener suscripciones: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Invoice getInvoice(String invoiceId) {
        try {
            com.stripe.model.Invoice stripeInvoice = com.stripe.model.Invoice.retrieve(invoiceId);
            return mapInvoiceToDomain(stripeInvoice);
        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error obteniendo factura: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al obtener factura: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public List<Invoice> getCustomerInvoices(String customerId, int limit) {
        try {
            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit)
                    .build();

            com.stripe.model.InvoiceCollection invoices = com.stripe.model.Invoice.list(params);

            return invoices.getData().stream()
                    .map(this::mapInvoiceToDomain)
                    .collect(Collectors.toList());

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error obteniendo facturas: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al obtener facturas: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    @Override
    public Invoice getUpcomingInvoice(String subscriptionId) {
        try {
            com.stripe.model.Subscription sub = com.stripe.model.Subscription.retrieve(subscriptionId);

            InvoiceUpcomingParams params = InvoiceUpcomingParams.builder()
                    .setCustomer(sub.getCustomer())
                    .setSubscription(subscriptionId)
                    .build();

            com.stripe.model.Invoice stripeInvoice = com.stripe.model.Invoice.upcoming(params);

            return mapInvoiceToDomain(stripeInvoice);

        } catch (StripeException e) {
            log.error("[Stripe Billing]  Error obteniendo próxima factura: {}", e.getMessage(), e);
            throw new BillingException(
                    "Error al obtener próxima factura: " + e.getUserMessage(),
                    e.getCode()
            );
        }
    }

    private Customer mapCustomerToDomain(com.stripe.model.Customer stripeCustomer) {
        return Customer.builder()
                .id(stripeCustomer.getId())
                .email(stripeCustomer.getEmail())
                .name(stripeCustomer.getName())
                .phone(stripeCustomer.getPhone())
                .defaultPaymentMethodId(stripeCustomer.getInvoiceSettings() != null
                        ? stripeCustomer.getInvoiceSettings().getDefaultPaymentMethod()
                        : null)
                .metadata(stripeCustomer.getMetadata())
                .createdAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeCustomer.getCreated()),
                        ZoneId.systemDefault()
                ))
                .build();
    }

    private Subscription mapSubscriptionToDomain(com.stripe.model.Subscription stripeSub) {
        com.stripe.model.Price price = stripeSub.getItems().getData().get(0).getPrice();

        return Subscription.builder()
                .id(stripeSub.getId())
                .customerId(stripeSub.getCustomer())
                .priceId(price.getId())
                .planName(price.getNickname())
                .status(Subscription.SubscriptionStatus.valueOf(stripeSub.getStatus().toUpperCase()))
                .amount(BigDecimal.valueOf(price.getUnitAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .currency(price.getCurrency())
                .interval(price.getRecurring() != null ? price.getRecurring().getInterval().toString() : null)
                .currentPeriodStart(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()),
                        ZoneId.systemDefault()
                ))
                .currentPeriodEnd(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()),
                        ZoneId.systemDefault()
                ))
                .cancelAtPeriodEnd(stripeSub.getCancelAtPeriodEnd())
                .canceledAt(stripeSub.getCanceledAt() != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getCanceledAt()), ZoneId.systemDefault())
                        : null)
                .trialStart(stripeSub.getTrialStart() != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getTrialStart()), ZoneId.systemDefault())
                        : null)
                .trialEnd(stripeSub.getTrialEnd() != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getTrialEnd()), ZoneId.systemDefault())
                        : null)
                .defaultPaymentMethodId(stripeSub.getDefaultPaymentMethod())
                .latestInvoiceId(stripeSub.getLatestInvoice())
                .metadata(stripeSub.getMetadata())
                .createdAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSub.getCreated()),
                        ZoneId.systemDefault()
                ))
                .build();
    }

    private Invoice mapInvoiceToDomain(com.stripe.model.Invoice stripeInvoice) {
        return Invoice.builder()
                .id(stripeInvoice.getId())
                .customerId(stripeInvoice.getCustomer())
                .subscriptionId(stripeInvoice.getSubscription())
                .status(Invoice.InvoiceStatus.valueOf(stripeInvoice.getStatus().toUpperCase()))
                .amountDue(BigDecimal.valueOf(stripeInvoice.getAmountDue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .amountPaid(BigDecimal.valueOf(stripeInvoice.getAmountPaid()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .amountRemaining(BigDecimal.valueOf(stripeInvoice.getAmountRemaining()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .currency(stripeInvoice.getCurrency())
                .number(stripeInvoice.getNumber())
                .hostedInvoiceUrl(stripeInvoice.getHostedInvoiceUrl())
                .invoicePdf(stripeInvoice.getInvoicePdf())
                .created(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeInvoice.getCreated()),
                        ZoneId.systemDefault()
                ))
                .dueDate(stripeInvoice.getDueDate() != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeInvoice.getDueDate()), ZoneId.systemDefault())
                        : null)
                .paidAt(stripeInvoice.getStatusTransitions() != null && stripeInvoice.getStatusTransitions().getPaidAt() != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeInvoice.getStatusTransitions().getPaidAt()), ZoneId.systemDefault())
                        : null)
                .metadata(stripeInvoice.getMetadata())
                .build();
    }
}
