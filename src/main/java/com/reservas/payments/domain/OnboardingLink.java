package com.reservas.payments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un link de onboarding para completar el perfil de Stripe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingLink {

    /**
     * URL del link de onboarding.
     * Este link expira después de cierto tiempo.
     */
    private String url;

    /**
     * Fecha de creación del link.
     */
    private LocalDateTime createdAt;

    /**
     * Fecha de expiración del link (típicamente 30 minutos).
     */
    private LocalDateTime expiresAt;
}
