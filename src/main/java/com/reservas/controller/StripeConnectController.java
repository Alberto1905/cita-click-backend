package com.reservas.controller;

import com.reservas.dto.response.ApiResponse;
import com.reservas.entity.StripeConnectedAccount;
import com.reservas.entity.Usuario;
import com.reservas.exception.PaymentException;
import com.reservas.payments.domain.OnboardingLink;
import com.reservas.payments.dto.CreateConnectedAccountRequest;
import com.reservas.payments.service.ConnectAccountService;
import com.reservas.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión de cuentas Stripe Connect
 */
@Slf4j
@RestController
@RequestMapping("/v1/stripe-connect")
@RequiredArgsConstructor
@Tag(name = "Stripe Connect", description = "Gestión de cuentas conectadas de Stripe")
@SecurityRequirement(name = "bearerAuth")
public class StripeConnectController {

    private final ConnectAccountService connectAccountService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.frontend.url:http://localhost:5174}")
    private String frontendUrl;

    /**
     * POST /api/v1/stripe-connect/account
     * Crea una cuenta Stripe Connect para el usuario
     */
    @PostMapping("/account")
    @Operation(
        summary = "Crear cuenta conectada",
        description = "Crea una cuenta Stripe Connect Standard para recibir pagos de clientes"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> crearCuenta(
            @Valid @RequestBody CreateConnectedAccountRequest request,
            Authentication authentication
    ) {
        log.info("[StripeConnectController] POST /api/v1/stripe-connect/account - Usuario: {}",
                authentication.getName());

        try {
            // Validar que el usuarioId corresponde al usuario autenticado
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));

            if (!usuario.getId().toString().equals(request.getUsuarioId())) {
                throw new PaymentException("No autorizado", "UNAUTHORIZED");
            }

            StripeConnectedAccount account = connectAccountService.createAccount(request);

            Map<String, Object> response = new HashMap<>();
            response.put("accountId", account.getId().toString());
            response.put("stripeAccountId", account.getStripeAccountId());
            response.put("accountType", account.getAccountType().toString());
            response.put("chargesEnabled", account.getChargesEnabled());
            response.put("payoutsEnabled", account.getPayoutsEnabled());
            response.put("onboardingCompleted", account.getOnboardingCompleted());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("Cuenta conectada creada exitosamente")
                            .data(response)
                            .build());
        } catch (PaymentException e) {
            log.error("[StripeConnectController] Error al crear cuenta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(Map.of("errorCode", e.getErrorCode()))
                            .build());
        }
    }

    /**
     * POST /api/v1/stripe-connect/onboarding-link
     * Genera un link para completar el onboarding de Stripe Connect
     */
    @PostMapping("/onboarding-link")
    @Operation(
        summary = "Generar link de onboarding",
        description = "Genera un link para que el usuario complete el proceso de onboarding de Stripe"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> generarOnboardingLink(
            Authentication authentication
    ) {
        log.info("[StripeConnectController] POST /api/v1/stripe-connect/onboarding-link - Usuario: {}",
                authentication.getName());

        try {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));

            String refreshUrl = frontendUrl + "/integrations?tab=stripe&refresh=true";
            String returnUrl = frontendUrl + "/integrations?tab=stripe&success=true";

            OnboardingLink link = connectAccountService.createOnboardingLink(
                    usuario.getId().toString(),
                    refreshUrl,
                    returnUrl
            );

            Map<String, Object> response = new HashMap<>();
            response.put("url", link.getUrl());
            response.put("expiresAt", link.getExpiresAt());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Link de onboarding generado exitosamente")
                    .data(response)
                    .build());
        } catch (PaymentException e) {
            log.error("[StripeConnectController] Error al generar link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(Map.of("errorCode", e.getErrorCode()))
                            .build());
        }
    }

    /**
     * GET /api/v1/stripe-connect/status
     * Obtiene el estado de la cuenta Stripe Connect del usuario
     */
    @GetMapping("/status")
    @Operation(
        summary = "Estado de la cuenta",
        description = "Obtiene el estado actual de la cuenta Stripe Connect"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstado(
            Authentication authentication
    ) {
        log.info("[StripeConnectController] GET /api/v1/stripe-connect/status - Usuario: {}",
                authentication.getName());

        try {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));

            StripeConnectedAccount account = connectAccountService.getAccountByUsuarioId(
                    usuario.getId().toString()
            );

            if (account == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("No tiene cuenta conectada")
                        .data(Map.of("hasAccount", false))
                        .build());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("hasAccount", true);
            response.put("accountId", account.getId().toString());
            response.put("stripeAccountId", account.getStripeAccountId());
            response.put("chargesEnabled", account.getChargesEnabled());
            response.put("payoutsEnabled", account.getPayoutsEnabled());
            response.put("onboardingCompleted", account.getOnboardingCompleted());
            response.put("active", account.getActive());
            response.put("readyForPayments", account.isReadyForPayments());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Estado obtenido exitosamente")
                    .data(response)
                    .build());
        } catch (PaymentException e) {
            log.error("[StripeConnectController] Error al obtener estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(Map.of("errorCode", e.getErrorCode()))
                            .build());
        }
    }

    /**
     * GET /api/v1/stripe-connect/account
     * Obtiene los detalles completos de la cuenta conectada
     */
    @GetMapping("/account")
    @Operation(
        summary = "Detalles de la cuenta",
        description = "Obtiene todos los detalles de la cuenta Stripe Connect"
    )
    public ResponseEntity<ApiResponse<StripeConnectedAccount>> obtenerCuenta(
            Authentication authentication
    ) {
        log.info("[StripeConnectController] GET /api/v1/stripe-connect/account - Usuario: {}",
                authentication.getName());

        try {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));

            StripeConnectedAccount account = connectAccountService.getAccountByUsuarioId(
                    usuario.getId().toString()
            );

            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<StripeConnectedAccount>builder()
                                .success(false)
                                .message("No se encontró cuenta conectada")
                                .build());
            }

            return ResponseEntity.ok(ApiResponse.<StripeConnectedAccount>builder()
                    .success(true)
                    .message("Cuenta obtenida exitosamente")
                    .data(account)
                    .build());
        } catch (PaymentException e) {
            log.error("[StripeConnectController] Error al obtener cuenta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<StripeConnectedAccount>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/v1/stripe-connect/refresh
     * Sincroniza el estado de la cuenta con Stripe
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Sincronizar cuenta",
        description = "Actualiza el estado de la cuenta con los datos más recientes de Stripe"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> sincronizarCuenta(
            Authentication authentication
    ) {
        log.info("[StripeConnectController] POST /api/v1/stripe-connect/refresh - Usuario: {}",
                authentication.getName());

        try {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new PaymentException("Usuario no encontrado", "USER_NOT_FOUND"));

            StripeConnectedAccount account = connectAccountService.getAccountByUsuarioId(
                    usuario.getId().toString()
            );

            if (account == null) {
                throw new PaymentException("No se encontró cuenta conectada", "ACCOUNT_NOT_FOUND");
            }

            connectAccountService.syncAccount(account.getStripeAccountId());

            // Volver a obtener la cuenta actualizada
            account = connectAccountService.getAccountByUsuarioId(usuario.getId().toString());

            Map<String, Object> response = new HashMap<>();
            response.put("chargesEnabled", account.getChargesEnabled());
            response.put("payoutsEnabled", account.getPayoutsEnabled());
            response.put("onboardingCompleted", account.getOnboardingCompleted());
            response.put("readyForPayments", account.isReadyForPayments());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Cuenta sincronizada exitosamente")
                    .data(response)
                    .build());
        } catch (PaymentException e) {
            log.error("[StripeConnectController] Error al sincronizar cuenta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(Map.of("errorCode", e.getErrorCode()))
                            .build());
        }
    }
}
