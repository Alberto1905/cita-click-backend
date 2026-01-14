package com.reservas.billing.provider;

import com.reservas.billing.domain.Subscription;
import com.reservas.billing.domain.Invoice;
import com.reservas.billing.domain.Customer;
import com.reservas.billing.dto.CreateSubscriptionRequest;
import com.reservas.billing.dto.CreateCustomerRequest;
import com.reservas.billing.dto.UpdateSubscriptionRequest;

import java.util.List;

/**
 * Interfaz para gestión de suscripciones del SaaS.
 *
 * Esta interfaz gestiona los COBROS A LOS USUARIOS DEL SAAS (no a los clientes finales).
 * Los pagos van a la cuenta principal de Stripe de la plataforma.
 *
 * SEPARACIÓN IMPORTANTE:
 * - SubscriptionProvider: Cobra a usuarios del SaaS (planes mensuales/anuales)
 * - PaymentProvider: Procesa pagos de clientes finales (con Connect)
 */
public interface SubscriptionProvider {

    /**
     * Crea un cliente en el sistema de billing.
     *
     * Un Customer en Stripe representa a un usuario del SaaS.
     * Se asocia con su email y se le pueden crear suscripciones.
     *
     * @param request Datos del usuario (email, nombre, etc.)
     * @return Customer creado con su ID
     * @throws BillingException si hay error
     */
    Customer createCustomer(CreateCustomerRequest request);

    /**
     * Crea una suscripción para un cliente.
     *
     * La suscripción se factura automáticamente según el plan:
     * - Mensual: cada mes
     * - Anual: cada año
     *
     * @param request Datos de la suscripción (customerId, priceId, etc.)
     * @return Subscription creada
     * @throws BillingException si hay error
     */
    Subscription createSubscription(CreateSubscriptionRequest request);

    /**
     * Actualiza una suscripción existente.
     *
     * Permite:
     * - Cambiar de plan (upgrade/downgrade)
     * - Actualizar cantidad
     * - Modificar método de pago
     *
     * @param subscriptionId ID de la suscripción
     * @param request Datos a actualizar
     * @return Subscription actualizada
     * @throws BillingException si hay error
     */
    Subscription updateSubscription(String subscriptionId, UpdateSubscriptionRequest request);

    /**
     * Cancela una suscripción.
     *
     * Opciones:
     * - Inmediata: cancela ahora
     * - Al final del periodo: cancela cuando expire el periodo actual
     *
     * @param subscriptionId ID de la suscripción
     * @param cancelAtPeriodEnd Si true, cancela al final del periodo
     * @return Subscription cancelada
     * @throws BillingException si hay error
     */
    Subscription cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd);

    /**
     * Reactiva una suscripción cancelada (solo si aún no ha expirado).
     *
     * @param subscriptionId ID de la suscripción
     * @return Subscription reactivada
     * @throws BillingException si ya expiró o hay error
     */
    Subscription reactivateSubscription(String subscriptionId);

    /**
     * Obtiene una suscripción por ID.
     *
     * @param subscriptionId ID de la suscripción
     * @return Subscription con datos actualizados
     * @throws BillingException si no existe
     */
    Subscription getSubscription(String subscriptionId);

    /**
     * Obtiene todas las suscripciones de un cliente.
     *
     * @param customerId ID del cliente
     * @return Lista de suscripciones (activas, canceladas, etc.)
     */
    List<Subscription> getCustomerSubscriptions(String customerId);

    /**
     * Obtiene una factura por ID.
     *
     * @param invoiceId ID de la factura
     * @return Invoice con detalles
     * @throws BillingException si no existe
     */
    Invoice getInvoice(String invoiceId);

    /**
     * Obtiene facturas de un cliente.
     *
     * @param customerId ID del cliente
     * @param limit Número máximo de facturas a retornar
     * @return Lista de facturas ordenadas por fecha
     */
    List<Invoice> getCustomerInvoices(String customerId, int limit);

    /**
     * Obtiene la próxima factura de una suscripción (sin crearla).
     *
     * Útil para mostrar al usuario cuánto pagará en el próximo ciclo.
     *
     * @param subscriptionId ID de la suscripción
     * @return Invoice con preview del próximo cobro
     * @throws BillingException si hay error
     */
    Invoice getUpcomingInvoice(String subscriptionId);
}
