package com.reservas.payments.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO para crear un PaymentIntent.
 *
 * Este request contiene toda la información necesaria para procesar
 * un pago de un cliente final a través de la cuenta conectada del usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /**
     * ID del usuario del SaaS que recibirá el pago.
     * Se usará para obtener su stripe_account_id.
     */
    @NotBlank(message = "Usuario ID es requerido")
    private String usuarioId;

    /**
     * Monto del pago en la moneda especificada.
     * Ejemplo: Para $10.50 USD enviar 10.50
     */
    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.50", message = "El monto mínimo es 0.50")
    private BigDecimal amount;

    /**
     * Moneda del pago (ISO 4217).
     */
    @NotBlank(message = "La moneda es requerida")
    @Size(min = 3, max = 3, message = "La moneda debe ser un código ISO de 3 letras")
    @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe estar en mayúsculas (ej: USD, MXN)")
    private String currency;

    /**
     * Porcentaje de comisión de la plataforma.
     * Ejemplo: 5.0 para 5%
     * Si es null, se usa el porcentaje por defecto configurado.
     */
    @DecimalMin(value = "0.0", message = "El porcentaje de comisión no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje de comisión no puede exceder 100%")
    private BigDecimal platformFeePercentage;

    /**
     * Email del cliente que realiza el pago.
     */
    @Email(message = "Email del cliente inválido")
    private String customerEmail;

    /**
     * Nombre del cliente que realiza el pago.
     */
    private String customerName;

    /**
     * Descripción del pago (aparece en el dashboard de Stripe).
     */
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;

    /**
     * ID de la cita asociada (opcional).
     */
    private String citaId;

    /**
     * Metadatos personalizados.
     * Se almacenan en Stripe y se pueden consultar en webhooks.
     */
    private Map<String, String> metadata;

    /**
     * Indica si se debe capturar el pago automáticamente.
     * true = captura automática (recomendado)
     * false = captura manual (para casos especiales)
     */
    @Builder.Default
    private Boolean captureMethod = true;

    /**
     * Indica si se debe confirmar el PaymentIntent inmediatamente.
     * true = confirmar de inmediato
     * false = se confirmará manualmente después
     */
    @Builder.Default
    private Boolean confirmPayment = false;
}
