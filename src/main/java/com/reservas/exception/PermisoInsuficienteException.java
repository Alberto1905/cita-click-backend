package com.reservas.exception;

/**
 * Excepción lanzada cuando un usuario no tiene permisos suficientes
 */
public class PermisoInsuficienteException extends RuntimeException {

    public PermisoInsuficienteException(String mensaje) {
        super(mensaje);
    }

    public PermisoInsuficienteException(String accion, String rol) {
        super(String.format("El rol '%s' no tiene permisos para: %s", rol, accion));
    }
}
