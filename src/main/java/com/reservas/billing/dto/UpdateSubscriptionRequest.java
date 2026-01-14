package com.reservas.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionRequest {

    private String priceId;
    private String paymentMethodId;
    private Boolean cancelAtPeriodEnd;
    private java.util.Map<String, String> metadata;
}
