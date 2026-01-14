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
     * Envía un email de verificación al usuario
     */
    @Transactional
    public void enviarEmailVerificacion(Usuario usuario) {
        // Generar token
        String token = generateVerificationToken();

        // Guardar token en el usuario
        usuario.setTokenVerificacion(token);
        usuario.setTokenVerificacionExpira(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        usuarioRepository.save(usuario);

        // Construir URL de verificación
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        // Construir mensaje
        String asunto = "Verifica tu correo electrónico - Cita Click";
        String mensaje = String.format(
                "Hola %s,\n\n" +
                "Gracias por registrarte en Cita Click.\n\n" +
                "Para activar tu cuenta, por favor verifica tu correo electrónico haciendo clic en el siguiente enlace:\n\n" +
                "%s\n\n" +
                "Este enlace expirará en 24 horas.\n\n" +
                "Si no creaste esta cuenta, puedes ignorar este mensaje.\n\n" +
                "Saludos,\n" +
                "El equipo de Cita Click",
                usuario.getNombre(),
                verificationUrl
        );

        // Enviar email
        emailService.enviarEmail(usuario.getEmail(), asunto, mensaje);

        log.info("[Email Verificación] Email de verificación enviado a: {}", usuario.getEmail());
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
