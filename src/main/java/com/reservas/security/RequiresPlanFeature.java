package com.reservas.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar endpoints que requieren una funcionalidad específica del plan
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPlanFeature {
    /**
     * Funcionalidad requerida
     * Opciones: "sms_whatsapp", "reportes_avanzados", "soporte_prioritario"
     */
    String value();

    /**
     * Mensaje personalizado cuando no se tiene acceso
     */
    String message() default "";
}
