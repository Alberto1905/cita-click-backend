package com.reservas.service;

import com.reservas.entity.Usuario;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Servicio para gestionar la verificación de correos electrónicos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5174}")
    private String frontendUrl;

    private static final int TOKEN_VALIDITY_HOURS = 24;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera un token de verificación único
     */
    private String generateVerificationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Guarda el token de verificación en el usuario y envía el email.
     * El guardado del token siempre se intenta; el envío del email nunca bloquea
     * el registro aunque falle (el usuario puede solicitar un reenvío).
     */
    @Transactional
    public void enviarEmailVerificacion(Usuario usuario) {
        // 1. Guardar token (dentro de la TX del registro)
        String token = generateVerificationToken();
        usuario.setTokenVerificacion(token);
        usuario.setTokenVerificacionExpira(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        usuarioRepository.save(usuario);
        log.info("[Email Verificación] Token generado y guardado para: {}", usuario.getEmail());

        // 2. Enviar email — cualquier fallo aquí no debe deshacer el registro
        try {
            String verificationUrl = frontendUrl + "/verify-email?token=" + token;
            boolean enviado = emailService.enviarEmailVerificacion(
                    usuario.getEmail(), usuario.getNombre(), verificationUrl);

            if (enviado) {
                log.info("[Email Verificación] Email enviado correctamente a: {}", usuario.getEmail());
            } else {
                log.warn("[Email Verificación] No se pudo enviar el email a: {}. " +
                         "El token está guardado; el usuario puede solicitar reenvío.", usuario.getEmail());
            }
        } catch (Exception e) {
            // Capturamos cualquier excepción inesperada para que NO marque la TX
            // como rollback-only y así el registro del usuario siempre se complete.
            log.error("[Email Verificación] Error inesperado enviando email a {}: {}",
                      usuario.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Verifica el email del usuario usando el token
     */
    @Transactional
    public boolean verificarEmail(String token) {
        log.info("[Email Verificación] Intentando verificar token: {}", token.substring(0, Math.min(10, token.length())) + "...");

        // Buscar usuario por token
        Usuario usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElse(null);

        if (usuario == null) {
            log.warn("[Email Verificación] Token no encontrado");
            return false;
        }

        // Verificar si el token ha expirado
        if (usuario.getTokenVerificacionExpira().isBefore(LocalDateTime.now())) {
            log.warn("[Email Verificación] Token expirado para usuario: {}", usuario.getEmail());
            return false;
        }

        // Marcar email como verificado
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpira(null);
        usuarioRepository.save(usuario);

        log.info("[Email Verificación]  Email verificado exitosamente para: {}", usuario.getEmail());
        return true;
    }

    /**
     * Reenvía el email de verificación
     */
    @Transactional
    public void reenviarEmailVerificacion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.isEmailVerificado()) {
            throw new RuntimeException("El email ya está verificado");
        }

        enviarEmailVerificacion(usuario);
        log.info("[Email Verificación] Email de verificación reenviado a: {}", email);
    }

    /**
     * Verifica si un usuario tiene su email verificado
     */
    public boolean isEmailVerificado(String email) {
        return usuarioRepository.findByEmail(email)
                .map(Usuario::isEmailVerificado)
                .orElse(false);
    }
}
