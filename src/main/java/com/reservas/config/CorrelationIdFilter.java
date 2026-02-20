package com.reservas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro que asigna un identificador único (correlationId) a cada request HTTP.
 * Permite rastrear todos los logs de un mismo request a través del MDC de SLF4J.
 *
 * El correlationId se propaga en la cabecera de respuesta X-Correlation-Id,
 * permitiendo al frontend correlacionar errores con trazas del backend.
 *
 * Orden -200: se ejecuta antes de Spring Security (-100) para que TODOS
 * los logs del request, incluidos los de seguridad, lleven el correlationId.
 */
@Component
@Order(-200)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USER_ID = "userId";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Reutilizar correlationId si el cliente lo envía (útil para reintentos o distributed tracing)
        String correlationId = request.getHeader(HEADER_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);

        // Propagar el correlationId en la respuesta para trazabilidad en el frontend
        response.addHeader(HEADER_CORRELATION_ID, correlationId);

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            // Ejecutar el resto de la cadena de filtros (Spring Security + controladores)
            filterChain.doFilter(request, response);
        } finally {
            // Después de que Spring Security autenticó al usuario, leer su identidad del SecurityContext
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    MDC.put(MDC_USER_ID, auth.getName());
                }
            } catch (Exception ignored) {
                // No interrumpir el flujo si no se puede obtener el usuario
            }

            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("{} {} → {} ({}ms)", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("{} {} → {} ({}ms)", method, uri, status, duration);
            } else {
                log.info("{} {} → {} ({}ms)", method, uri, status, duration);
            }

            // CRÍTICO: limpiar MDC para evitar fugas entre requests en el thread pool
            MDC.clear();
        }
    }
}
