package com.reservas.payments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa una cuenta conectada en el dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedAccount {

    /**
     * ID de la cuenta en el proveedor (acct_xxxxx en Stripe).
     */
    private String id;

    /**
     * Tipo de cuenta.
     */
    private String type;

    /**
     * Email asociado.
     */
    private String email;

    /**
     * País.
     */
    private String country;

    /**
     * Indica si puede recibir cargos.
     */
    private Boolean chargesEnabled;

    /**
     * Indica si puede hacer payouts.
     */
    private Boolean payoutsEnabled;

    /**
     * Indica si el onboarding está completo.
     */
    private Boolean detailsSubmitted;

    /**
     * Requisitos pendientes.
     */
    private AccountRequirements requirements;

    /**
     * Información de la cuenta bancaria.
     */
    private BankAccountInfo bankAccount;

    /**
     * Metadatos.
     */
    private java.util.Map<String, String> metadata;

    /**
     * Fecha de creación.
     */
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountRequirements {
        /**
         * Campos actualmente vencidos.
         */
        private List<String> currentlyDue;

        /**
         * Campos que vencerán pronto.
         */
        private List<String> eventuallyDue;

        /**
         * Campos con errores.
         */
        private List<String> errors;

        /**
         * Fecha límite para completar.
         */
        private LocalDateTime currentDeadline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankAccountInfo {
        /**
         * Nombre del banco.
         */
        private String bankName;

        /**
         * Últimos 4 dígitos de la cuenta.
         */
        private String last4;

        /**
         * País del banco.
         */
        private String country;

        /**
         * Moneda de la cuenta.
         */
        private String currency;
    }
}
