package com.reservas.payments.service;

import com.reservas.entity.StripeConnectedAccount;
import com.reservas.entity.Usuario;
import com.reservas.exception.PaymentException;
import com.reservas.payments.domain.ConnectedAccount;
import com.reservas.payments.domain.OnboardingLink;
import com.reservas.payments.dto.CreateConnectedAccountRequest;
import com.reservas.payments.stripe.StripeConnectAccountProvider;
import com.reservas.repository.StripeConnectedAccountRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio para gestionar cuentas conectadas de Stripe.
 *
 * Orquesta la lógica entre el provider de Stripe y la persistencia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectAccountService {

    private final StripeConnectAccountProvider connectAccountProvider;
    private final StripeConnectedAccountRepository accountRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea una cuenta conectada para un usuario.
     */
    @Transactional
    public StripeConnectedAccount createAccount(CreateConnectedAccountRequest request) {
        log.info("Creando cuenta conectada para usuario: {}", request.getUsuarioId());

        Usuario usuario = usuarioRepository.findById(java.util.UUID.fromString(request.getUsuarioId()))
                .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));

        // Validar plan Premium
        if (usuario.getNegocio() == null || !"premium".equalsIgnoreCase(usuario.getNegocio().getPlan())) {
            throw new PaymentException("Stripe Connect requiere plan Premium", "PLAN_NOT_PREMIUM");
        }

        if (accountRepository.existsByUsuarioId(java.util.UUID.fromString(request.getUsuarioId()))) {
            throw new PaymentException("El usuario ya tiene una cuenta conectada", "ACCOUNT_ALREADY_EXISTS");
        }

        ConnectedAccount stripeAccount = connectAccountProvider.createStandardAccount(request);

        StripeConnectedAccount account = StripeConnectedAccount.builder()
                .usuario(usuario)
                .stripeAccountId(stripeAccount.getId())
                .accountType(StripeConnectedAccount.AccountType.STANDARD)
                .email(stripeAccount.getEmail())
                .country(stripeAccount.getCountry())
                .chargesEnabled(stripeAccount.getChargesEnabled())
                .payoutsEnabled(stripeAccount.getPayoutsEnabled())
                .onboardingCompleted(stripeAccount.getDetailsSubmitted())
                .active(true)
                .build();

        StripeConnectedAccount saved = accountRepository.save(account);
        log.info("Cuenta conectada creada exitosamente: {}", saved.getStripeAccountId());

        return saved;
    }

    /**
     * Genera un link de onboarding para completar la configuración de la cuenta.
     */
    public OnboardingLink createOnboardingLink(String usuarioId, String refreshUrl, String returnUrl) {
        log.info("Generando link de onboarding para usuario: {}", usuarioId);

        // Validar plan Premium
        Usuario usuarioEntity = usuarioRepository.findById(java.util.UUID.fromString(usuarioId))
                .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));
        if (usuarioEntity.getNegocio() == null || !"premium".equalsIgnoreCase(usuarioEntity.getNegocio().getPlan())) {
            throw new PaymentException("Stripe Connect requiere plan Premium", "PLAN_NOT_PREMIUM");
        }

        StripeConnectedAccount account = accountRepository.findByUsuarioId(java.util.UUID.fromString(usuarioId))
                .orElseThrow(() -> new PaymentException("Cuenta no encontrada", "ACCOUNT_NOT_FOUND"));

        OnboardingLink link = connectAccountProvider.createOnboardingLink(
                account.getStripeAccountId(),
                refreshUrl,
                returnUrl
        );

        log.info("Link de onboarding generado para cuenta: {}", account.getStripeAccountId());
        return link;
    }

    /**
     * Sincroniza el estado de una cuenta desde Stripe.
     * Se llama típicamente desde webhooks.
     */
    @Transactional
    public void syncAccount(String stripeAccountId) {
        log.info("Sincronizando cuenta: {}", stripeAccountId);

        ConnectedAccount stripeAccount = connectAccountProvider.getAccount(stripeAccountId);

        StripeConnectedAccount account = accountRepository.findByStripeAccountId(stripeAccountId)
                .orElseThrow(() -> new PaymentException("Cuenta no encontrada", "ACCOUNT_NOT_FOUND"));

        account.setChargesEnabled(stripeAccount.getChargesEnabled());
        account.setPayoutsEnabled(stripeAccount.getPayoutsEnabled());
        account.setOnboardingCompleted(stripeAccount.getDetailsSubmitted());

        if (stripeAccount.getRequirements() != null) {
            account.setRequirementsPending(
                    stripeAccount.getRequirements().getCurrentlyDue() != null
                            ? String.join(",", stripeAccount.getRequirements().getCurrentlyDue())
                            : null
            );
        }

        accountRepository.save(account);
        log.info("Cuenta sincronizada: {}", stripeAccountId);
    }

    /**
     * Verifica si la cuenta está lista para recibir pagos.
     */
    public boolean isAccountReady(String usuarioId) {
        return accountRepository.findByUsuarioId(java.util.UUID.fromString(usuarioId))
                .map(StripeConnectedAccount::isReadyForPayments)
                .orElse(false);
    }

    /**
     * Obtiene la cuenta conectada de un usuario.
     */
    public StripeConnectedAccount getAccountByUsuario(String usuarioId) {
        return accountRepository.findByUsuarioId(java.util.UUID.fromString(usuarioId))
                .orElseThrow(() -> new PaymentException("Cuenta no encontrada", "ACCOUNT_NOT_FOUND"));
    }

    /**
     * Obtiene la cuenta conectada de un usuario (retorna null si no existe).
     */
    public StripeConnectedAccount getAccountByUsuarioId(String usuarioId) {
        return accountRepository.findByUsuarioId(java.util.UUID.fromString(usuarioId))
                .orElse(null);
    }
}
