package com.reservas.security;

import com.reservas.exception.LimiteExcedidoException;
import com.reservas.service.PlanLimitesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspecto para validar funcionalidades del plan antes de ejecutar un método
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanFeatureAspect {

    private final PlanLimitesService planLimitesService;

    @Before("@annotation(com.reservas.security.RequiresPlanFeature)")
    public void checkPlanFeature(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPlanFeature annotation = method.getAnnotation(RequiresPlanFeature.class);

        if (annotation == null) {
            return;
        }

        String funcionalidad = annotation.value();
        String customMessage = annotation.message();

        // Obtener el email del usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Usuario no autenticado intentando acceder a funcionalidad: {}", funcionalidad);
            throw new LimiteExcedidoException("Debe iniciar sesión para acceder a esta funcionalidad", 0, 0);
        }

        String email = authentication.getName();
        log.info("Validando funcionalidad '{}' para usuario: {}", funcionalidad, email);

        // Validar funcionalidad usando PlanLimitesService
        boolean tieneAcceso = planLimitesService.validarFuncionalidadPorEmail(email, funcionalidad);

        if (!tieneAcceso) {
            String mensaje = customMessage.isEmpty()
                    ? String.format("Esta funcionalidad ('%s') no está disponible en su plan actual. Actualice su plan para acceder.", funcionalidad)
                    : customMessage;

            log.warn("Acceso denegado a funcionalidad '{}' para usuario: {}", funcionalidad, email);
            throw new LimiteExcedidoException(mensaje, 0, 0);
        }

        log.info("Acceso concedido a funcionalidad '{}' para usuario: {}", funcionalidad, email);
    }
}
