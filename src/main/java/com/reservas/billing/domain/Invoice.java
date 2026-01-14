package com.reservas.billing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa una factura en el dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    private String id;
    private String customerId;
    private String subscriptionId;
    private InvoiceStatus status;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private String currency;
    private String number;
    private String hostedInvoiceUrl;
    private String invoicePdf;
    private LocalDateTime created;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private List<InvoiceLineItem> lineItems;
    private java.util.Map<String, String> metadata;

    public enum InvoiceStatus {
        DRAFT,
        OPEN,
        PAID,
        VOID,
        UNCOLLECTIBLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLineItem {
        private String id;
        private String description;
        private BigDecimal amount;
        private String currency;
        private Integer quantity;
    }
}
