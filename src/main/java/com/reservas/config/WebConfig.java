package com.reservas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reservas.security.SuscripcionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de interceptores HTTP para la aplicación.
 * Registra el interceptor de validación de suscripciones.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SuscripcionInterceptor suscripcionInterceptor;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(suscripcionInterceptor)
                .addPathPatterns("/api/**")                     // Interceptar todas las rutas /api
                .excludePathPatterns(
                        "/api/auth/**",                         // Excluir autenticación
                        "/api/public/**",                       // Excluir endpoints públicos
                        "/api/suscripcion/info",                // Permitir consultar info
                        "/api/suscripcion/activar",             // Permitir activar suscripción
                        "/api/pagos/**",                        // Permitir procesar pagos
                        "/api/webhook/**",                      // Permitir webhooks
                        "/api/health",                          // Health check
                        "/api/status"                           // Status check
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir archivos subidos (logos) de forma estática
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
