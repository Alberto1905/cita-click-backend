package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa una cuenta conectada de Stripe Connect.
 *
 * Cada usuario del SaaS que quiera recibir pagos necesita una cuenta conectada.
 * Esta entidad almacena la relación entre el usuario del SaaS y su cuenta de Stripe.
 *
 * IMPORTANTE:
 * - Un usuario puede tener solo UNA cuenta conectada activa
 * - El stripe_account_id es el que se usa en los PaymentIntents
 * - charges_enabled debe ser true para poder recibir pagos
 */
@Entity
@Table(name = "tbl_stripe_connected_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeConnectedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Usuario del SaaS propietario de esta cuenta.
     * Un usuario solo puede tener una cuenta activa.
     */
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /**
     * ID de la cuenta en Stripe (acct_xxxxx).
     * Este es el ID que se pasa en las llamadas a la API de Stripe.
     */
    @Column(name = "stripe_account_id", nullable = false, unique = true, length = 100)
    private String stripeAccountId;

    /**
     * Tipo de cuenta Stripe Connect.
     * En producción debería ser siempre STANDARD.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /**
     * Email asociado a la cuenta Stripe.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * País de la cuenta (ISO 3166-1 alpha-2).
     * Ejemplo: "MX", "US", "ES"
     */
    @Column(name = "country", length = 2)
    private String country;

    /**
     * Indica si la cuenta puede recibir cargos (charges).
     * Solo debe ser true después de completar el onboarding.
     */
    @Column(name = "charges_enabled", nullable = false)
    private Boolean chargesEnabled;

    /**
     * Indica si la cuenta puede hacer payouts (transferencias bancarias).
     */
    @Column(name = "payouts_enabled", nullable = false)
    private Boolean payoutsEnabled;

    /**
     * Indica si el onboarding ha sido completado.
     */
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted;

    /**
     * Indica si la cuenta está activa.
     * Se usa para soft-delete o desactivar temporalmente.
     */
    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * Requisitos pendientes de Stripe (JSON).
     * Stripe devuelve qué información falta para completar la verificación.
     */
    @Column(name = "requirements_pending", columnDefinition = "TEXT")
    private String requirementsPending;

    /**
     * Último error de Stripe al intentar verificar la cuenta.
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * Metadatos adicionales (JSON).
     * Útil para guardar información personalizada.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Tipo de cuenta Stripe Connect
     */
    public enum AccountType {
        /**
         * Cuenta Standard: El usuario tiene su propio dashboard de Stripe.
         * Recomendado para SaaS donde los usuarios gestionan sus pagos.
         */
        STANDARD,

        /**
         * Cuenta Express: Stripe gestiona el onboarding simplificado.
         */
        EXPRESS,

        /**
         * Cuenta Custom: Control total de la plataforma.
         * Requiere gestionar toda la lógica de compliance.
         */
        CUSTOM
    }

    /**
     * Método helper para verificar si la cuenta está lista para recibir pagos.
     */
    public boolean isReadyForPayments() {
        return Boolean.TRUE.equals(active)
                && Boolean.TRUE.equals(chargesEnabled)
                && Boolean.TRUE.equals(onboardingCompleted);
    }
}
