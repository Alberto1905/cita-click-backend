package com.reservas.service;

import com.reservas.dto.CambiarRolRequest;
import com.reservas.dto.InvitarUsuarioRequest;
import com.reservas.dto.UsuarioDTO;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.entity.enums.TipoPlan;
import com.reservas.entity.enums.UsuarioRol;
import com.reservas.exception.LimiteExcedidoException;
import com.reservas.exception.PermisoInsuficienteException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios con validación de límites y permisos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final PlanLimitesService planLimitesService;
    private final PermisosService permisosService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Invita un nuevo usuario al negocio
     */
    @Transactional
    public UsuarioDTO invitarUsuario(InvitarUsuarioRequest request, String emailActual) {
        log.info("[UsuarioService] Invitando usuario - Solicitado por: {}", emailActual);

        // Obtener usuario actual
        Usuario usuarioActual = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID negocioId = usuarioActual.getNegocio().getId();
        log.info("[UsuarioService] Invitando usuario al negocio: {}", negocioId);

        // 1. Validar que el usuario actual tenga permisos
        permisosService.validarPermiso(usuarioActual, "INVITAR_USUARIOS");

        // 2. Validar que puede gestionar el rol objetivo
        permisosService.validarGestionRol(usuarioActual, request.getRol());

        // 3. Obtener negocio
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado"));

        // 4. Validar límite de usuarios del plan
        TipoPlan plan = TipoPlan.fromCodigo(negocio.getPlan());
        planLimitesService.validarLimiteUsuarios(negocioId, plan);

        // 5. Verificar que el email no esté en uso en este negocio
        if (usuarioRepository.findByNegocioAndEmailAndActivo(negocio, request.getEmail(), true).isPresent()) {
            log.warn("[UsuarioService] Email ya existe en el negocio: {}", request.getEmail());
            throw new IllegalArgumentException("Ya existe un usuario activo con este email en el negocio");
        }

        // 6. Validar rol
        try {
            UsuarioRol.fromCodigo(request.getRol());
        } catch (IllegalArgumentException e) {
            log.error("[UsuarioService] Rol no válido: {}", request.getRol());
            throw new IllegalArgumentException("Rol no válido: " + request.getRol());
        }

        // 7. Generar password temporal si no se proveyó
        String password = request.getPasswordTemporal();
        if (password == null || password.isEmpty()) {
            password = generarPasswordTemporal();
        }

        // 8. Crear usuario
        Usuario nuevoUsuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .passwordHash(passwordEncoder.encode(password))
                .rol(request.getRol())
                .negocio(negocio)
                .activo(true)
                .authProvider("local")
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        log.info("[UsuarioService] Usuario creado exitosamente: {} con rol: {}",
                usuarioGuardado.getEmail(), usuarioGuardado.getRol());

        // 9. Actualizar uso
        planLimitesService.actualizarUso(negocioId);

        // 10. Enviar email de bienvenida con credenciales temporales
        try {
            emailService.enviarEmailInvitacionUsuario(
                    usuarioGuardado.getEmail(),
                    usuarioGuardado.getNombre(),
                    negocio.getNombre(),
                    password
            );
            log.info("[UsuarioService] Email de invitación enviado a: {}", usuarioGuardado.getEmail());
        } catch (Exception e) {
            log.error("[UsuarioService] Error al enviar email de invitación", e);
            // No fallar la operación por error de email
        }

        return convertirADTO(usuarioGuardado);
    }

    /**
     * Lista todos los usuarios activos de un negocio
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuariosPorNegocio(String emailActual) {
        log.info("[UsuarioService] Listando usuarios - Solicitado por: {}", emailActual);

        // JOIN FETCH previene LazyInitializationException
        Usuario usuarioActual = usuarioRepository.findByEmailWithNegocio(emailActual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID negocioId = usuarioActual.getNegocio().getId();
        log.info("[UsuarioService] Listando usuarios del negocio: {}", negocioId);

        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado"));

        List<Usuario> usuarios = usuarioRepository.findByNegocioAndActivo(negocio, true);

        log.info("[UsuarioService] Encontrados {} usuarios activos", usuarios.size());
        return usuarios.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Cambia el rol de un usuario
     */
    @Transactional
    public UsuarioDTO cambiarRol(UUID usuarioId, CambiarRolRequest request, String emailActual) {
        log.info("[UsuarioService] Cambiando rol del usuario: {} a rol: {}", usuarioId, request.getRol());

        // Obtener usuario actual
        Usuario usuarioActual = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 1. Validar permisos
        permisosService.validarPermiso(usuarioActual, "CAMBIAR_ROL_USUARIOS");

        // 2. Obtener usuario objetivo
        Usuario usuarioObjetivo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 3. Verificar que pertenecen al mismo negocio
        if (!usuarioActual.getNegocio().getId().equals(usuarioObjetivo.getNegocio().getId())) {
            log.warn("[UsuarioService] Intento de cambiar rol de usuario de otro negocio");
            throw new PermisoInsuficienteException("No puedes gestionar usuarios de otro negocio");
        }

        // 4. Validar que puede gestionar el rol actual y el nuevo rol
        permisosService.validarGestionRol(usuarioActual, usuarioObjetivo.getRol());
        permisosService.validarGestionRol(usuarioActual, request.getRol());

        // 5. No permitir que el owner se quite el rol a sí mismo
        if (permisosService.esOwner(usuarioObjetivo) && usuarioObjetivo.getId().equals(usuarioActual.getId())) {
            log.warn("[UsuarioService] El owner no puede cambiar su propio rol");
            throw new IllegalArgumentException("No puedes cambiar tu propio rol de OWNER");
        }

        // 6. Validar el nuevo rol
        try {
            UsuarioRol.fromCodigo(request.getRol());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no válido: " + request.getRol());
        }

        // 7. Cambiar rol
        String rolAnterior = usuarioObjetivo.getRol();
        usuarioObjetivo.setRol(request.getRol());
        Usuario usuarioActualizado = usuarioRepository.save(usuarioObjetivo);

        log.info("[UsuarioService] Rol cambiado exitosamente de {} a {}", rolAnterior, request.getRol());

        return convertirADTO(usuarioActualizado);
    }

    /**
     * Desactiva un usuario
     */
    @Transactional
    public void desactivarUsuario(UUID usuarioId, String emailActual) {
        log.info("[UsuarioService] Desactivando usuario: {}", usuarioId);

        // Obtener usuario actual
        Usuario usuarioActual = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 1. Validar permisos
        permisosService.validarPermiso(usuarioActual, "DESACTIVAR_USUARIOS");

        // 2. Obtener usuario
        Usuario usuarioObjetivo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 3. Verificar mismo negocio
        if (!usuarioActual.getNegocio().getId().equals(usuarioObjetivo.getNegocio().getId())) {
            throw new PermisoInsuficienteException("No puedes gestionar usuarios de otro negocio");
        }

        // 4. Validar que puede gestionar ese rol
        permisosService.validarGestionRol(usuarioActual, usuarioObjetivo.getRol());

        // 5. No permitir que el owner se desactive a sí mismo
        if (permisosService.esOwner(usuarioObjetivo) && usuarioObjetivo.getId().equals(usuarioActual.getId())) {
            throw new IllegalArgumentException("No puedes desactivarte a ti mismo siendo OWNER");
        }

        // 6. No permitir desactivar al único owner
        if (permisosService.esOwner(usuarioObjetivo)) {
            long ownersActivos = usuarioRepository.findByNegocioIdAndRol(
                    usuarioObjetivo.getNegocio().getId(),
                    UsuarioRol.OWNER.getCodigo()
            ).stream().filter(Usuario::isActivo).count();

            if (ownersActivos <= 1) {
                throw new IllegalArgumentException("No puedes desactivar al único OWNER del negocio");
            }
        }

        // 7. Desactivar
        usuarioObjetivo.setActivo(false);
        usuarioRepository.save(usuarioObjetivo);

        log.info("[UsuarioService] Usuario desactivado exitosamente");

        // 8. Actualizar uso
        planLimitesService.actualizarUso(usuarioObjetivo.getNegocio().getId());
    }

    /**
     * Obtiene un usuario por ID
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(UUID usuarioId, String emailActual) {
        log.info("[UsuarioService] Obteniendo usuario: {}", usuarioId);

        // JOIN FETCH previene LazyInitializationException
        Usuario usuarioActual = usuarioRepository.findByEmailWithNegocio(emailActual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar que pertenecen al mismo negocio
        if (!usuarioActual.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new PermisoInsuficienteException("No tienes acceso a este usuario");
        }

        return convertirADTO(usuario);
    }

    /**
     * Activa un usuario previamente desactivado
     */
    @Transactional
    public UsuarioDTO activarUsuario(UUID usuarioId, String emailActual) {
        log.info("[UsuarioService] Activando usuario: {}", usuarioId);

        // Obtener usuario actual
        Usuario usuarioActual = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 1. Validar permisos
        permisosService.validarPermiso(usuarioActual, "INVITAR_USUARIOS");

        // 2. Obtener usuario
        Usuario usuarioObjetivo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 3. Verificar mismo negocio
        if (!usuarioActual.getNegocio().getId().equals(usuarioObjetivo.getNegocio().getId())) {
            throw new PermisoInsuficienteException("No puedes gestionar usuarios de otro negocio");
        }

        // 4. Validar que puede gestionar ese rol
        permisosService.validarGestionRol(usuarioActual, usuarioObjetivo.getRol());

        // 5. Validar límite de usuarios antes de reactivar
        if (!usuarioObjetivo.isActivo()) {
            TipoPlan plan = TipoPlan.fromCodigo(usuarioObjetivo.getNegocio().getPlan());
            planLimitesService.validarLimiteUsuarios(usuarioObjetivo.getNegocio().getId(), plan);
        }

        // 6. Activar
        usuarioObjetivo.setActivo(true);
        Usuario usuarioReactivado = usuarioRepository.save(usuarioObjetivo);

        log.info("[UsuarioService] Usuario reactivado exitosamente");

        // 7. Actualizar uso
        planLimitesService.actualizarUso(usuarioObjetivo.getNegocio().getId());

        return convertirADTO(usuarioReactivado);
    }

    /**
     * Genera un password temporal aleatorio
     */
    private String generarPasswordTemporal() {
        // Generar password seguro de 12 caracteres
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }

    /**
     * Convierte entidad a DTO
     */
    private UsuarioDTO convertirADTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidoPaterno(usuario.getApellidoPaterno())
                .apellidoMaterno(usuario.getApellidoMaterno())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .authProvider(usuario.getAuthProvider())
                .imageUrl(usuario.getImageUrl())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
