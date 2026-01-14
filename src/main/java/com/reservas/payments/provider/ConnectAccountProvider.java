package com.reservas.payments.provider;

import com.reservas.payments.domain.ConnectedAccount;
import com.reservas.payments.domain.OnboardingLink;
import com.reservas.payments.dto.CreateConnectedAccountRequest;

/**
 * Interfaz para gestión de cuentas conectadas (Stripe Connect).
 *
 * Esta interfaz maneja todo lo relacionado con:
 * - Creación de cuentas Standard para usuarios del SaaS
 * - Onboarding de usuarios
 * - Verificación de estado de cuentas
 * - Gestión del ciclo de vida de cuentas conectadas
 */
public interface ConnectAccountProvider {

    /**
     * Crea una cuenta conectada tipo STANDARD.
     *
     * En Stripe Connect Standard:
     * - El usuario tiene su propia cuenta Stripe
     * - Recibe pagos directamente
     * - La plataforma cobra comisión vía application_fee
     * - El usuario ve su dashboard de Stripe
     *
     * @param request Datos del usuario (email, país, tipo de negocio)
     * @return ConnectedAccount con stripe_account_id
     * @throws PaymentException si hay error en la creación
     */
    ConnectedAccount createStandardAccount(CreateConnectedAccountRequest request);

    /**
     * Genera un link de onboarding para que el usuario complete su perfil.
     *
     * El link dirige al usuario al flujo de Stripe Hosted Onboarding donde:
     * - Proporciona información de negocio
     * - Verifica identidad
     * - Conecta cuenta bancaria
     *
     * @param accountId ID de la cuenta conectada (stripe_account_id)
     * @param refreshUrl URL a donde redirigir si el usuario necesita actualizar info
     * @param returnUrl URL a donde redirigir cuando complete el onboarding
     * @return OnboardingLink con URL temporal
     * @throws PaymentException si hay error generando el link
     */
    OnboardingLink createOnboardingLink(String accountId, String refreshUrl, String returnUrl);

    /**
     * Obtiene el estado actual de una cuenta conectada.
     *
     * Verifica:
     * - Si está completamente verificada
     * - Si puede recibir pagos (charges_enabled)
     * - Si puede hacer payouts (payouts_enabled)
     * - Qué información falta por completar
     *
     * @param accountId ID de la cuenta conectada
     * @return ConnectedAccount con datos actualizados
     * @throws PaymentException si no existe o hay error
     */
    ConnectedAccount getAccount(String accountId);

    /**
     * Valida que una cuenta esté habilitada para recibir pagos.
     *
     * CRÍTICO: Siempre validar antes de crear un PaymentIntent
     * para evitar errores en producción.
     *
     * @param accountId ID de la cuenta conectada
     * @return true si charges_enabled = true
     * @throws PaymentException si hay error consultando
     */
    boolean isAccountEnabled(String accountId);

    /**
     * Actualiza información de una cuenta conectada.
     *
     * Permite actualizar metadatos, configuraciones, etc.
     * NO permite actualizar datos sensibles que deben hacerse vía Stripe Dashboard.
     *
     * @param accountId ID de la cuenta
     * @param metadata Metadatos a actualizar
     * @return ConnectedAccount actualizado
     * @throws PaymentException si hay error
     */
    ConnectedAccount updateAccount(String accountId, java.util.Map<String, String> metadata);

    /**
     * Desactiva/elimina una cuenta conectada.
     *
     * CUIDADO: Esta operación puede ser irreversible según el proveedor.
     *
     * @param accountId ID de la cuenta a desactivar
     * @throws PaymentException si hay error
     */
    void deleteAccount(String accountId);
}
