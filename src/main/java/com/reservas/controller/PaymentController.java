package com.reservas.controller;

import com.reservas.dto.response.ApiResponse;
import com.reservas.entity.Payment;
import com.reservas.exception.PaymentException;
import com.reservas.payments.dto.CreatePaymentRequest;
import com.reservas.payments.dto.RefundRequest;
import com.reservas.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión de pagos de clientes finales a través de Stripe Connect
 */
@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestión de pagos de clientes finales")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments
     * Crea un nuevo PaymentIntent para cobrar a un cliente
     */
    @PostMapping
    @Operation(
        summary = "Crear pago",
        description = "Crea un PaymentIntent de Stripe para cobrar a un cliente final por una cita o servicio"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> crearPago(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        log.info("[PaymentController] POST /api/v1/payments - Amount: {} {} - Usuario: {}",
                request.getAmount(), request.getCurrency(), authentication.getName());

        try {
            Payment payment = paymentService.createPayment(request);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId().toString());
            response.put("paymentIntentId", payment.getPaymentIntentId());
            response.put("clientSecret", payment.getClientSecret());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("platformFee", payment.getPlatformFee());
            response.put("netAmount", payment.getNetAmount());
            response.put("status", payment.getStatus().toString());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("Payment Intent creado exitosamente")
                            .data(response)
                            .build());
        } catch (PaymentException e) {
            log.error("[PaymentController] Error al crear pago: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(Map.of("errorCode", e.getErrorCode()))
                            .build());
        }
    }

    /**
     * GET /api/v1/payments
     * Obtiene el historial de pagos del usuario
     */
    @GetMapping
    @Operation(
        summary = "Listar pagos",
        description = "Obtiene el historial de pagos recibidos de clientes"
    )
    public ResponseEntity<ApiResponse<Page<Payment>>> listarPagos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        log.info("[PaymentController] GET /api/v1/payments - Usuario: {} - Page: {} - Size: {} - Status: {}",
                authentication.getName(), page, size, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Payment> payments = status != null && !status.isEmpty()
                ? paymentService.findByUsuarioEmailAndStatus(authentication.getName(), status, pageable)
                : paymentService.findByUsuarioEmail(authentication.getName(), pageable);

        return ResponseEntity.ok(ApiResponse.<Page<Payment>>builder()
                .success(true)
                .message("Pagos obtenidos exitosamente")
                .data(payments)
                .build());
    }

    /**
     * GET /api/v1/payments/{paymentId}
     * Obtiene los detalles de un pago específico
     */
    @GetMapping("/{paymentId}")
    @Operation(
        summary = "Obtener pago",
        description = "Obtiene los detalles de un pago específico"
    )
    public ResponseEntity<ApiResponse<Payment>> obtenerPago(
            @PathVariable String paymentId,
            Authentication authentication
    ) {
        log.info("[PaymentController] GET /api/v1/payments/{} - Usuario: {}",
                paymentId, authentication.getName());

        Payment payment = paymentService.findById(paymentId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.<Payment>builder()
                .success(true)
                .message("Pago obtenido exitosamente")
                .data(payment)
                .build());
    }

    /**
     * POST /api/v1/payments/refund
     * Procesa un reembolso para un pago
     */
    @PostMapping("/refund")
    @Operation(
        summary = "Procesar reembolso",
        description = "Reembolsa total o parcialmente un pago a un cliente"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> procesarReembolso(
            @Valid @RequestBody RefundRequest request,
            Authentication authentication
    ) {
        log.info("[PaymentController] POST /api/v1/payments/refund - PaymentId: {} - Amount: {} - Usuario: {}",
                request.getPaymentId(), request.getAmount(), authentication.getName());

        try {
            Payment payment = paymentService.createRefund(request, authentication.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId().toString());
            response.put("refundId", payment.getRefundId());
            response.put("refundAmount", payment.getRefundAmount());
            response.put("refundReason", payment.getRefundReason());
            response.put("status", payment.getStatus().toString());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Reembolso procesado exitosamente")
                    .data(response)
                    .build());
        } catch (PaymentException e) {
            log.error("[PaymentController] Error al procesar reembolso: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(Map.of("errorCode", e.getErrorCode()))
                            .build());
        }
    }

    /**
     * GET /api/v1/payments/public/{paymentId}
     * Obtiene información básica de un pago SIN autenticación.
     * Usado por la página de pago del cliente final (/pay/:paymentId).
     * NO expone datos sensibles del negocio.
     */
    @GetMapping("/public/{paymentId}")
    @Operation(
        summary = "Obtener pago público",
        description = "Retorna información básica del pago para que el cliente final pueda completar el pago"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerPagoPublico(
            @PathVariable String paymentId
    ) {
        log.info("[PaymentController] GET /api/v1/payments/public/{}", paymentId);

        try {
            Payment payment = paymentService.getPaymentById(paymentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", payment.getId());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("description", payment.getDescription());
            response.put("customerName", payment.getCustomerName());
            response.put("status", payment.getStatus().toString());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Pago obtenido exitosamente")
                    .data(response)
                    .build());
        } catch (PaymentException e) {
            log.error("[PaymentController] Pago público no encontrado: {}", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Pago no encontrado")
                            .build());
        }
    }

    /**
     * GET /api/v1/payments/statistics
     * Obtiene estadísticas de pagos
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Estadísticas de pagos",
        description = "Obtiene estadísticas y métricas de pagos recibidos"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticas(
            Authentication authentication
    ) {
        log.info("[PaymentController] GET /api/v1/payments/statistics - Usuario: {}",
                authentication.getName());

        Map<String, Object> stats = paymentService.getPaymentStatistics(authentication.getName());

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Estadísticas obtenidas exitosamente")
                .data(stats)
                .build());
    }
}
