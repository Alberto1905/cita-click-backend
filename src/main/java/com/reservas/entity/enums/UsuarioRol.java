package com.reservas.entity.enums;

import lombok.Getter;

@Getter
public enum UsuarioRol {
    OWNER("owner", "Propietario del negocio"),
    ADMIN("admin", "Administrador"),
    EMPLEADO("empleado", "Empleado"),
    RECEPCIONISTA("recepcionista", "Recepcionista");

    private final String codigo;
    private final String descripcion;

    UsuarioRol(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static UsuarioRol fromCodigo(String codigo) {
        for (UsuarioRol rol : values()) {
            if (rol.codigo.equalsIgnoreCase(codigo)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + codigo);
    }
}
