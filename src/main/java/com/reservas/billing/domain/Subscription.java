package com.reservas.billing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una suscripción en el dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    private String id;
    private String customerId;
    private String priceId;
    private String planName;
    private SubscriptionStatus status;
    private BigDecimal amount;
    private String currency;
    private String interval;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private Boolean cancelAtPeriodEnd;
    private LocalDateTime canceledAt;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private String defaultPaymentMethodId;
    private String latestInvoiceId;
    private java.util.Map<String, String> metadata;
    private LocalDateTime createdAt;

    public enum SubscriptionStatus {
        ACTIVE,
        TRIALING,
        INCOMPLETE,
        INCOMPLETE_EXPIRED,
        PAST_DUE,
        CANCELED,
        UNPAID
    }
}
