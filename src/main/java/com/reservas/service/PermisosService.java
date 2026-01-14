package com.reservas.service;

import com.reservas.entity.Usuario;
import com.reservas.entity.enums.UsuarioRol;
import com.reservas.exception.PermisoInsuficienteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Servicio para gestionar permisos por rol
 */
@Slf4j
@Service
public class PermisosService {

    // Matriz de permisos: Rol -> Set de permisos
    private static final Map<UsuarioRol, Set<String>> MATRIZ_PERMISOS = new HashMap<>();

    static {
        // OWNER - Tiene todos los permisos
        Set<String> permisosOwner = new HashSet<>();
        permisosOwner.add("GESTIONAR_USUARIOS");
        permisosOwner.add("INVITAR_USUARIOS");
        permisosOwner.add("CAMBIAR_ROL_USUARIOS");
        permisosOwner.add("DESACTIVAR_USUARIOS");
        permisosOwner.add("CAMBIAR_PLAN");
        permisosOwner.add("VER_REPORTES");
        permisosOwner.add("DESCARGAR_REPORTES");
        permisosOwner.add("CREAR_CITAS");
        permisosOwner.add("CANCELAR_CITAS");
        permisosOwner.add("MODIFICAR_CITAS");
        permisosOwner.add("GESTIONAR_CLIENTES");
        permisosOwner.add("CREAR_CLIENTES");
        permisosOwner.add("MODIFICAR_CLIENTES");
        permisosOwner.add("ELIMINAR_CLIENTES");
        permisosOwner.add("GESTIONAR_SERVICIOS");
        permisosOwner.add("CREAR_SERVICIOS");
        permisosOwner.add("MODIFICAR_SERVICIOS");
        permisosOwner.add("ELIMINAR_SERVICIOS");
        permisosOwner.add("CONFIGURAR_NEGOCIO");
        permisosOwner.add("VER_METRICAS");
        permisosOwner.add("VER_DASHBOARD");
        permisosOwner.add("GESTIONAR_HORARIOS");
        MATRIZ_PERMISOS.put(UsuarioRol.OWNER, permisosOwner);

        // ADMIN - Casi todos los permisos excepto cambiar plan y gestionar owners
        Set<String> permisosAdmin = new HashSet<>();
        permisosAdmin.add("GESTIONAR_USUARIOS");
        permisosAdmin.add("INVITAR_USUARIOS");
        permisosAdmin.add("CAMBIAR_ROL_USUARIOS"); // Solo empleados y recepcionistas
        permisosAdmin.add("DESACTIVAR_USUARIOS"); // Solo empleados y recepcionistas
        permisosAdmin.add("VER_REPORTES");
        permisosAdmin.add("DESCARGAR_REPORTES");
        permisosAdmin.add("CREAR_CITAS");
        permisosAdmin.add("CANCELAR_CITAS");
        permisosAdmin.add("MODIFICAR_CITAS");
        permisosAdmin.add("GESTIONAR_CLIENTES");
        permisosAdmin.add("CREAR_CLIENTES");
        permisosAdmin.add("MODIFICAR_CLIENTES");
        permisosAdmin.add("ELIMINAR_CLIENTES");
        permisosAdmin.add("GESTIONAR_SERVICIOS");
        permisosAdmin.add("CREAR_SERVICIOS");
        permisosAdmin.add("MODIFICAR_SERVICIOS");
        permisosAdmin.add("ELIMINAR_SERVICIOS");
        permisosAdmin.add("CONFIGURAR_NEGOCIO");
        permisosAdmin.add("VER_METRICAS");
        permisosAdmin.add("VER_DASHBOARD");
        permisosAdmin.add("GESTIONAR_HORARIOS");
        MATRIZ_PERMISOS.put(UsuarioRol.ADMIN, permisosAdmin);

        // EMPLEADO - Permisos operativos
        Set<String> permisosEmpleado = new HashSet<>();
        permisosEmpleado.add("VER_REPORTES");
        permisosEmpleado.add("CREAR_CITAS");
        permisosEmpleado.add("CANCELAR_CITAS");
        permisosEmpleado.add("MODIFICAR_CITAS");
        permisosEmpleado.add("GESTIONAR_CLIENTES");
        permisosEmpleado.add("CREAR_CLIENTES");
        permisosEmpleado.add("MODIFICAR_CLIENTES");
        permisosEmpleado.add("VER_METRICAS");
        permisosEmpleado.add("VER_DASHBOARD");
        MATRIZ_PERMISOS.put(UsuarioRol.EMPLEADO, permisosEmpleado);

        // RECEPCIONISTA - Permisos básicos
        Set<String> permisosRecepcionista = new HashSet<>();
        permisosRecepcionista.add("CREAR_CITAS");
        permisosRecepcionista.add("CANCELAR_CITAS");
        permisosRecepcionista.add("MODIFICAR_CITAS");
        permisosRecepcionista.add("GESTIONAR_CLIENTES");
        permisosRecepcionista.add("CREAR_CLIENTES");
        permisosRecepcionista.add("MODIFICAR_CLIENTES");
        permisosRecepcionista.add("VER_DASHBOARD");
        MATRIZ_PERMISOS.put(UsuarioRol.RECEPCIONISTA, permisosRecepcionista);
    }

    /**
     * Verifica si un usuario tiene un permiso específico
     */
    public boolean tienePermiso(Usuario usuario, String permiso) {
        if (usuario == null || usuario.getRol() == null) {
            log.warn("[PermisosService] Usuario o rol nulo");
            return false;
        }

        try {
            UsuarioRol rol = UsuarioRol.fromCodigo(usuario.getRol());
            Set<String> permisos = MATRIZ_PERMISOS.get(rol);

            if (permisos == null) {
                log.warn("[PermisosService] No se encontraron permisos para rol: {}", rol);
                return false;
            }

            boolean tienePermiso = permisos.contains(permiso);
            log.debug("[PermisosService] Usuario {} (rol: {}) - Permiso '{}': {}",
                    usuario.getEmail(), rol, permiso, tienePermiso);

            return tienePermiso;
        } catch (IllegalArgumentException e) {
            log.error("[PermisosService] Rol no válido: {}", usuario.getRol(), e);
            return false;
        }
    }

    /**
     * Valida que un usuario tenga un permiso, lanza excepción si no lo tiene
     */
    public void validarPermiso(Usuario usuario, String permiso) {
        log.info("[PermisosService] Validando permiso '{}' para usuario: {}", permiso, usuario.getEmail());

        if (!tienePermiso(usuario, permiso)) {
            log.warn("[PermisosService] Permiso DENEGADO - Usuario: {}, Rol: {}, Permiso: {}",
                    usuario.getEmail(), usuario.getRol(), permiso);
            throw new PermisoInsuficienteException(permiso, usuario.getRol());
        }

        log.info("[PermisosService] Permiso CONCEDIDO");
    }

    /**
     * Obtiene todos los permisos de un rol
     */
    public Set<String> obtenerPermisos(UsuarioRol rol) {
        Set<String> permisos = MATRIZ_PERMISOS.get(rol);
        return permisos != null ? new HashSet<>(permisos) : new HashSet<>();
    }

    /**
     * Verifica si un rol puede gestionar a otro rol
     * OWNER puede gestionar a todos
     * ADMIN puede gestionar a EMPLEADO y RECEPCIONISTA
     */
    public boolean puedeGestionarRol(UsuarioRol rolActual, UsuarioRol rolObjetivo) {
        log.debug("[PermisosService] Verificando si {} puede gestionar a {}", rolActual, rolObjetivo);

        if (rolActual == UsuarioRol.OWNER) {
            return true; // Owner puede gestionar a todos
        }

        if (rolActual == UsuarioRol.ADMIN) {
            // Admin puede gestionar solo a empleados y recepcionistas
            return rolObjetivo == UsuarioRol.EMPLEADO || rolObjetivo == UsuarioRol.RECEPCIONISTA;
        }

        return false; // Empleados y recepcionistas no pueden gestionar a nadie
    }

    /**
     * Valida que un usuario pueda gestionar a otro
     */
    public void validarGestionRol(Usuario usuarioActual, String rolObjetivo) {
        log.info("[PermisosService] Validando gestión de rol '{}' por usuario: {}",
                rolObjetivo, usuarioActual.getEmail());

        UsuarioRol rolActual = UsuarioRol.fromCodigo(usuarioActual.getRol());
        UsuarioRol rolTarget = UsuarioRol.fromCodigo(rolObjetivo);

        if (!puedeGestionarRol(rolActual, rolTarget)) {
            log.warn("[PermisosService] Gestión de rol DENEGADA - Rol actual: {}, Rol objetivo: {}",
                    rolActual, rolTarget);
            throw new PermisoInsuficienteException(
                    String.format("No tienes permisos para gestionar usuarios con rol '%s'", rolTarget.getDescripcion())
            );
        }

        log.info("[PermisosService] Gestión de rol PERMITIDA");
    }

    /**
     * Verifica si un usuario es owner del negocio
     */
    public boolean esOwner(Usuario usuario) {
        try {
            UsuarioRol rol = UsuarioRol.fromCodigo(usuario.getRol());
            return rol == UsuarioRol.OWNER;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifica si un usuario es admin o owner
     */
    public boolean esAdminOOwner(Usuario usuario) {
        try {
            UsuarioRol rol = UsuarioRol.fromCodigo(usuario.getRol());
            return rol == UsuarioRol.OWNER || rol == UsuarioRol.ADMIN;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
