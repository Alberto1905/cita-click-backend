package com.reservas.payments.provider;

import com.reservas.payments.domain.PaymentIntent;
import com.reservas.payments.domain.PaymentResult;
import com.reservas.payments.domain.Refund;
import com.reservas.payments.dto.CreatePaymentRequest;
import com.reservas.payments.dto.RefundRequest;

import java.math.BigDecimal;

/**
 * Interfaz base para proveedores de pago.
 *
 * Esta interfaz permite desacoplar la lógica de negocio del proveedor específico (Stripe, etc.).
 * Sigue el principio de Inversión de Dependencias (SOLID).
 *
 * Los métodos de esta interfaz representan las operaciones fundamentales para gestionar
 * pagos en la plataforma.
 */
public interface PaymentProvider {

    /**
     * Crea un intento de pago (PaymentIntent) para cobrar a un cliente final.
     *
     * Este método se usa cuando un cliente de uno de los usuarios del SaaS
     * realiza una compra/pago.
     *
     * @param request Datos del pago (monto, usuario propietario, cliente, etc.)
     * @return PaymentIntent creado con su ID y estado
     * @throws PaymentException si hay error en la creación
     */
    PaymentIntent createPaymentIntent(CreatePaymentRequest request);

    /**
     * Confirma un PaymentIntent previamente creado.
     *
     * @param paymentIntentId ID del PaymentIntent a confirmar
     * @param idempotencyKey Clave de idempotencia para evitar duplicados
     * @return Resultado del pago con estado actualizado
     * @throws PaymentException si hay error en la confirmación
     */
    PaymentResult confirmPayment(String paymentIntentId, String idempotencyKey);

    /**
     * Cancela un PaymentIntent que aún no ha sido confirmado.
     *
     * @param paymentIntentId ID del PaymentIntent a cancelar
     * @return PaymentIntent cancelado
     * @throws PaymentException si hay error en la cancelación
     */
    PaymentIntent cancelPayment(String paymentIntentId);

    /**
     * Crea un reembolso (total o parcial) de un pago.
     *
     * @param request Datos del reembolso (ID del pago, monto, razón)
     * @return Refund creado
     * @throws PaymentException si hay error en el reembolso
     */
    Refund createRefund(RefundRequest request);

    /**
     * Obtiene el estado actual de un PaymentIntent.
     *
     * @param paymentIntentId ID del PaymentIntent
     * @return PaymentIntent con datos actualizados
     * @throws PaymentException si no existe o hay error
     */
    PaymentIntent getPaymentIntent(String paymentIntentId);

    /**
     * Calcula la comisión de la plataforma sobre un monto.
     *
     * Este método permite centralizar el cálculo de comisiones
     * y facilitar cambios futuros en la estructura de comisiones.
     *
     * @param amount Monto base del pago
     * @param platformFeePercentage Porcentaje de comisión de la plataforma
     * @return Monto de la comisión en la menor unidad de la moneda (centavos)
     */
    Long calculatePlatformFee(BigDecimal amount, BigDecimal platformFeePercentage);

    /**
     * Verifica la firma de un webhook para validar su autenticidad.
     *
     * CRÍTICO PARA SEGURIDAD: Este método debe validar que el webhook
     * realmente proviene del proveedor de pagos y no ha sido manipulado.
     *
     * @param payload Cuerpo del webhook (JSON raw)
     * @param signature Firma del webhook (header)
     * @param secret Secret del webhook configurado
     * @return true si la firma es válida, false en caso contrario
     */
    boolean verifyWebhookSignature(String payload, String signature, String secret);
}
