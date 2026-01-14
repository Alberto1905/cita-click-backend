package com.reservas.security;

import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.SuscripcionVencidaException;
import com.reservas.repository.UsuarioRepository;
import com.reservas.service.SuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor para validar el estado de suscripción antes de permitir acceso a endpoints protegidos.
 * Retorna HTTP 402 Payment Required si la suscripción ha expirado.
 */
@Component
@RequiredArgsConstructor
public class SuscripcionInterceptor implements HandlerInterceptor {

    private final SuscripcionService suscripcionService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Permitir OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Obtener la ruta de la petición
        String path = request.getRequestURI();

        // Excluir rutas públicas que no requieren validación de suscripción
        if (esRutaExcluida(path)) {
            return true;
        }

        // Obtener el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            // Si no está autenticado, Spring Security se encargará
            return true;
        }

        // Obtener el email del usuario autenticado
        String email = authentication.getName();

        // Buscar el usuario y su negocio
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new RuntimeException("El usuario no tiene un negocio asociado");
        }

        // Validar el acceso según el estado de la suscripción
        try {
            suscripcionService.validarAcceso(email);
            return true; // Permitir acceso
        } catch (SuscripcionVencidaException e) {
            // Suscripción vencida - retornar 402 Payment Required
            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = String.format(
                "{\"error\": \"Suscripción vencida\", \"message\": \"%s\", \"statusCode\": 402}",
                e.getMessage()
            );

            response.getWriter().write(jsonResponse);
            return false; // Bloquear acceso
        }
    }

    /**
     * Determina si una ruta está excluida de la validación de suscripción
     */
    private boolean esRutaExcluida(String path) {
        // Rutas públicas que no requieren suscripción activa
        return path.startsWith("/api/auth/") ||                    // Autenticación
               path.startsWith("/api/public/") ||                  // Endpoints públicos
               path.startsWith("/api/suscripcion/activar") ||      // Activar suscripción
               path.startsWith("/api/suscripcion/info") ||         // Info de suscripción
               path.startsWith("/api/pagos/") ||                   // Procesar pagos
               path.startsWith("/api/webhook/") ||                 // Webhooks de Stripe
               path.equals("/api/health") ||                       // Health check
               path.equals("/api/status");                         // Status check
    }
}
