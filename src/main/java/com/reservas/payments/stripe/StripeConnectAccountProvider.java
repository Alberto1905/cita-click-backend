package com.reservas.payments.stripe;

import com.reservas.exception.PaymentException;
import com.reservas.payments.domain.ConnectedAccount;
import com.reservas.payments.domain.OnboardingLink;
import com.reservas.payments.dto.CreateConnectedAccountRequest;
import com.reservas.payments.provider.ConnectAccountProvider;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.AccountUpdateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de Stripe Connect para gestión de cuentas conectadas.
 *
 * Esta implementación usa cuentas STANDARD, donde:
 * - El usuario tiene su propia cuenta Stripe
 * - Ve su dashboard de Stripe
 * - La plataforma cobra comisión vía application_fee_amount
 * - Los pagos van directamente a la cuenta del usuario
 *
 * IMPORTANTE:
 * - NUNCA exponer la API key en código
 * - SIEMPRE usar variables de entorno
 * - VALIDAR que las cuentas estén habilitadas antes de cobrar
 */
@Slf4j
@Service
public class StripeConnectAccountProvider implements ConnectAccountProvider {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.platform.name:Cita Click}")
    private String platformName;

    @PostConstruct
    public void init() {
        // Configurar Stripe con la API key
        Stripe.apiKey = stripeSecretKey;
        log.info("[Stripe Connect] Inicializado correctamente");
    }

    @Override
    public ConnectedAccount createStandardAccount(CreateConnectedAccountRequest request) {
        try {
            log.info("[Stripe Connect] Creando cuenta Standard para usuario: {}", request.getUsuarioId());

            // Preparar metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("usuario_id", request.getUsuarioId());
            metadata.put("platform", platformName);
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }

            // Configurar el tipo de cuenta
            AccountCreateParams.BusinessType businessType = request.getBusinessType() != null
                    && request.getBusinessType().equalsIgnoreCase("company")
                    ? AccountCreateParams.BusinessType.COMPANY
                    : AccountCreateParams.BusinessType.INDIVIDUAL;

            // Crear cuenta Standard en Stripe
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.STANDARD)
                    .setCountry(request.getCountry())
                    .setEmail(request.getEmail())
                    .setBusinessType(businessType)
                    .putAllMetadata(metadata)
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setCardPayments(
                                            AccountCreateParams.Capabilities.CardPayments.builder()
                                                    .setRequested(true)
                                                    .build()
                                    )
                                    .setTransfers(
                                            AccountCreateParams.Capabilities.Transfers.builder()
                                                    .setRequested(true)
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Account account = Account.create(params);

            log.info("[Stripe Connect]  Cuenta creada exitosamente: {}", account.getId());

            // Mapear a nuestro dominio
            return mapToDomain(account);

        } catch (StripeException e) {
            log.error("[Stripe Connect]  Error creando cuenta: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al crear cuenta conectada: " + e.getUserMessage(),
                    "STRIPE_CONNECT_CREATE_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public OnboardingLink createOnboardingLink(String accountId, String refreshUrl, String returnUrl) {
        try {
            log.info("[Stripe Connect] Creando link de onboarding para: {}", accountId);

            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(params);

            log.info("[Stripe Connect]  Link de onboarding creado");

            return OnboardingLink.builder()
                    .url(accountLink.getUrl())
                    .createdAt(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(accountLink.getCreated()),
                            ZoneId.systemDefault()
                    ))
                    .expiresAt(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(accountLink.getExpiresAt()),
                            ZoneId.systemDefault()
                    ))
                    .build();

        } catch (StripeException e) {
            log.error("[Stripe Connect]  Error creando link de onboarding: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al crear link de onboarding: " + e.getUserMessage(),
                    "STRIPE_ONBOARDING_LINK_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public ConnectedAccount getAccount(String accountId) {
        try {
            log.debug("[Stripe Connect] Obteniendo cuenta: {}", accountId);

            Account account = Account.retrieve(accountId);

            return mapToDomain(account);

        } catch (StripeException e) {
            log.error("[Stripe Connect]  Error obteniendo cuenta: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al obtener cuenta conectada: " + e.getUserMessage(),
                    "STRIPE_CONNECT_GET_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public boolean isAccountEnabled(String accountId) {
        try {
            Account account = Account.retrieve(accountId);

            boolean enabled = account.getChargesEnabled() != null && account.getChargesEnabled();

            log.debug("[Stripe Connect] Cuenta {} - charges_enabled: {}", accountId, enabled);

            return enabled;

        } catch (StripeException e) {
            log.error("[Stripe Connect]  Error verificando cuenta: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public ConnectedAccount updateAccount(String accountId, Map<String, String> metadata) {
        try {
            log.info("[Stripe Connect] Actualizando cuenta: {}", accountId);

            AccountUpdateParams params = AccountUpdateParams.builder()
                    .putAllMetadata(metadata)
                    .build();

            Account account = Account.retrieve(accountId);
            account = account.update(params);

            log.info("[Stripe Connect]  Cuenta actualizada");

            return mapToDomain(account);

        } catch (StripeException e) {
            log.error("[Stripe Connect]  Error actualizando cuenta: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al actualizar cuenta conectada: " + e.getUserMessage(),
                    "STRIPE_CONNECT_UPDATE_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public void deleteAccount(String accountId) {
        try {
            log.warn("[Stripe Connect] Eliminando cuenta: {}", accountId);

            Account account = Account.retrieve(accountId);
            account.delete();

            log.info("[Stripe Connect]  Cuenta eliminada");

        } catch (StripeException e) {
            log.error("[Stripe Connect]  Error eliminando cuenta: {}", e.getMessage(), e);
            throw new PaymentException(
                    "Error al eliminar cuenta conectada: " + e.getUserMessage(),
                    "STRIPE_CONNECT_DELETE_ERROR",
                    e.getMessage()
            );
        }
    }

    /**
     * Mapea un Account de Stripe a nuestro dominio ConnectedAccount.
     */
    private ConnectedAccount mapToDomain(Account account) {
        ConnectedAccount.AccountRequirements requirements = null;

        if (account.getRequirements() != null) {
            requirements = ConnectedAccount.AccountRequirements.builder()
                    .currentlyDue(account.getRequirements().getCurrentlyDue())
                    .eventuallyDue(account.getRequirements().getEventuallyDue())
                    .errors(account.getRequirements().getErrors() != null
                            ? account.getRequirements().getErrors().stream()
                            .map(error -> error.getReason())
                            .collect(Collectors.toList())
                            : null)
                    .currentDeadline(account.getRequirements().getCurrentDeadline() != null
                            ? LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(account.getRequirements().getCurrentDeadline()),
                            ZoneId.systemDefault())
                            : null)
                    .build();
        }

        ConnectedAccount.BankAccountInfo bankInfo = null;
        if (account.getExternalAccounts() != null && !account.getExternalAccounts().getData().isEmpty()) {
            var externalAccount = account.getExternalAccounts().getData().get(0);
            if (externalAccount instanceof com.stripe.model.BankAccount) {
                com.stripe.model.BankAccount bankAccount = (com.stripe.model.BankAccount) externalAccount;
                bankInfo = ConnectedAccount.BankAccountInfo.builder()
                        .bankName(bankAccount.getBankName())
                        .last4(bankAccount.getLast4())
                        .country(bankAccount.getCountry())
                        .currency(bankAccount.getCurrency())
                        .build();
            }
        }

        return ConnectedAccount.builder()
                .id(account.getId())
                .type(account.getType())
                .email(account.getEmail())
                .country(account.getCountry())
                .chargesEnabled(account.getChargesEnabled())
                .payoutsEnabled(account.getPayoutsEnabled())
                .detailsSubmitted(account.getDetailsSubmitted())
                .requirements(requirements)
                .bankAccount(bankInfo)
                .metadata(account.getMetadata())
                .createdAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(account.getCreated()),
                        ZoneId.systemDefault()
                ))
                .build();
    }
}
