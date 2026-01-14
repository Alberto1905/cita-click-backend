package com.reservas.controller;

import com.reservas.dto.ActivarSuscripcionRequest;
import com.reservas.dto.SuscripcionInfoResponse;
import com.reservas.entity.Negocio;
import com.reservas.service.SuscripcionInfoService;
import com.reservas.service.SuscripcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para gestionar suscripciones y pagos.
 * Delega toda la lógica de negocio a servicios transaccionales.
 */
@Slf4j
@RestController
@RequestMapping("/suscripcion")
@RequiredArgsConstructor
public class SuscripcionController {

    private final SuscripcionService suscripcionService;
    private final SuscripcionInfoService suscripcionInfoService;

    /**
     * Obtiene la información de la suscripción del usuario autenticado.
     * Esta ruta está excluida del interceptor para permitir consultar el estado incluso si está vencida.
     *
     * SOLUCIÓN LAZY LOADING:
     * - Delega a servicio @Transactional(readOnly = true)
     * - Usa JOIN FETCH en repositorio para cargar Negocio eagerly
     * - Previene LazyInitializationException completamente
     */
    @GetMapping("/info")
    public ResponseEntity<SuscripcionInfoResponse> obtenerInfoSuscripcion(Authentication authentication) {
        log.info("[SuscripcionController] Solicitando info de suscripción para: {}", authentication.getName());

        String email = authentication.getName();

        // Delegamos al servicio transaccional que maneja correctamente el lazy loading
        SuscripcionInfoResponse response = suscripcionInfoService.obtenerInfoSuscripcion(email);

        return ResponseEntity.ok(response);
    }

    /**
     * Activa una suscripción después de que el usuario haya realizado un pago exitoso.
     * Esta ruta está excluida del interceptor para permitir activar incluso si está vencida.
     */
    @PostMapping("/activar")
    public ResponseEntity<?> activarSuscripcion(
            @Valid @RequestBody ActivarSuscripcionRequest request,
            Authentication authentication) {

        log.info("[SuscripcionController] Activando suscripción para: {}", authentication.getName());

        String email = authentication.getName();

        // Obtenemos el negocio dentro del contexto transaccional
        Negocio negocio = suscripcionInfoService.obtenerNegocioPorEmail(email);

        // Activar la suscripción
        suscripcionService.activarSuscripcion(negocio.getId().toString(), request.getPlan());

        log.info("[SuscripcionController] Suscripción activada exitosamente - Plan: {}", request.getPlan());

        // Retornar información actualizada
        return ResponseEntity.ok().body(
                new MessageResponse("Suscripción activada exitosamente. ¡Bienvenido de vuelta!")
        );
    }

    /**
     * Endpoint de prueba para verificar el estado del scheduler (solo para desarrollo/testing)
     * NOTA: Este endpoint debería estar protegido o removido en producción
     */
    @GetMapping("/test/verificar")
    public ResponseEntity<?> testVerificarSuscripciones() {
        suscripcionService.verificarSuscripcionesVencidas();
        return ResponseEntity.ok().body(
                new MessageResponse("Verificación ejecutada manualmente")
        );
    }

    /**
     * Endpoint de prueba para enviar notificaciones (solo para desarrollo/testing)
     * NOTA: Este endpoint debería estar protegido o removido en producción
     */
    @GetMapping("/test/notificaciones")
    public ResponseEntity<?> testEnviarNotificaciones() {
        suscripcionService.enviarNotificaciones();
        return ResponseEntity.ok().body(
                new MessageResponse("Notificaciones enviadas manualmente")
        );
    }

    // Clase interna para respuestas simples
    private record MessageResponse(String message) {}
}
