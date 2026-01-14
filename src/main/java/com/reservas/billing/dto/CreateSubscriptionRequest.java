package com.reservas.billing.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank(message = "Customer ID es requerido")
    private String customerId;

    @NotBlank(message = "Price ID es requerido (del plan en Stripe)")
    private String priceId;

    private String paymentMethodId;
    private Integer trialPeriodDays;
    private LocalDateTime trialEnd;
    private java.util.Map<String, String> metadata;
}
