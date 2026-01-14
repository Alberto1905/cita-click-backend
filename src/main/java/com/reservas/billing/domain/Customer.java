package com.reservas.billing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un cliente en el sistema de billing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private String id;
    private String email;
    private String name;
    private String phone;
    private String defaultPaymentMethodId;
    private String invoicePrefix;
    private java.util.Map<String, String> metadata;
    private LocalDateTime createdAt;
}
