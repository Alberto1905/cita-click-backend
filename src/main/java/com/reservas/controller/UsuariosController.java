package com.reservas.controller;

import com.reservas.dto.CambiarRolRequest;
import com.reservas.dto.InvitarUsuarioRequest;
import com.reservas.dto.UsuarioDTO;
import com.reservas.dto.response.ApiResponse;
import com.reservas.entity.Usuario;
import com.reservas.security.JwtProvider;
import com.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para gestión de usuarios del negocio
 * Endpoints para invitar, listar, cambiar roles y desactivar usuarios
 */
@RestController
@RequestMapping("/usuarios")
@Slf4j
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtProvider tokenProvider;

    /**
     * POST /api/usuarios/invitar
     * Invita a un nuevo usuario al negocio
     * Solo OWNER y ADMIN pueden invitar
     */
    @PostMapping("/invitar")
    public ResponseEntity<ApiResponse<UsuarioDTO>> invitarUsuario(
            @RequestBody @Valid InvitarUsuarioRequest request,
            @RequestHeader("Authorization") String token) {

        log.info(" Solicitud para invitar usuario: {}", request.getEmail());

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        UsuarioDTO usuarioDTO = usuarioService.invitarUsuario(request, email);

        log.info(" Usuario invitado exitosamente: {}", usuarioDTO.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UsuarioDTO>builder()
                        .success(true)
                        .message("Usuario invitado exitosamente. Se ha enviado un correo con las credenciales.")
                        .data(usuarioDTO)
                        .build());
    }

    /**
     * GET /api/usuarios
     * Lista todos los usuarios del negocio
     * OWNER ve todos, ADMIN ve empleados/recepcionistas, otros solo se ven a sí mismos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> listarUsuarios(
            @RequestHeader("Authorization") String token) {

        log.info("📋 Solicitud para listar usuarios");

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        List<UsuarioDTO> usuarios = usuarioService.listarUsuariosPorNegocio(email);

        log.info(" Usuarios listados: {}", usuarios.size());

        return ResponseEntity.ok(ApiResponse.<List<UsuarioDTO>>builder()
                .success(true)
                .message("Usuarios obtenidos exitosamente")
                .data(usuarios)
                .build());
    }

    /**
     * GET /api/usuarios/{id}
     * Obtiene un usuario específico por ID
     * Valida permisos de acceso
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerUsuario(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        log.info("🔍 Solicitud para obtener usuario: {}", id);

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorId(UUID.fromString(id), email);

        log.info(" Usuario obtenido: {}", usuario.getEmail());

        return ResponseEntity.ok(ApiResponse.<UsuarioDTO>builder()
                .success(true)
                .message("Usuario obtenido exitosamente")
                .data(usuario)
                .build());
    }

    /**
     * PUT /api/usuarios/{id}/rol
     * Cambia el rol de un usuario
     * Solo OWNER y ADMIN pueden cambiar roles (con restricciones)
     */
    @PutMapping("/{id}/rol")
    public ResponseEntity<ApiResponse<UsuarioDTO>> cambiarRol(
            @PathVariable String id,
            @RequestBody @Valid CambiarRolRequest request,
            @RequestHeader("Authorization") String token) {

        log.info(" Solicitud para cambiar rol del usuario: {} a {}", id, request.getRol());

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        UsuarioDTO usuario = usuarioService.cambiarRol(UUID.fromString(id), request, email);

        log.info(" Rol cambiado exitosamente: {} -> {}", usuario.getEmail(), usuario.getRol());

        return ResponseEntity.ok(ApiResponse.<UsuarioDTO>builder()
                .success(true)
                .message("Rol actualizado exitosamente")
                .data(usuario)
                .build());
    }

    /**
     * DELETE /api/usuarios/{id}
     * Desactiva un usuario (soft delete)
     * Solo OWNER y ADMIN pueden desactivar usuarios
     * No se puede desactivar al único OWNER
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivarUsuario(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        log.info("🗑️ Solicitud para desactivar usuario: {}", id);

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        usuarioService.desactivarUsuario(UUID.fromString(id), email);

        log.info(" Usuario desactivado exitosamente: {}", id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Usuario desactivado exitosamente")
                .build());
    }

    /**
     * PUT /api/usuarios/{id}/activar
     * Reactiva un usuario previamente desactivado
     * Solo OWNER y ADMIN pueden reactivar
     */
    @PutMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<UsuarioDTO>> activarUsuario(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        log.info("♻️ Solicitud para reactivar usuario: {}", id);

        String email = tokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        UsuarioDTO usuario = usuarioService.activarUsuario(UUID.fromString(id), email);

        log.info(" Usuario reactivado exitosamente: {}", usuario.getEmail());

        return ResponseEntity.ok(ApiResponse.<UsuarioDTO>builder()
                .success(true)
                .message("Usuario reactivado exitosamente")
                .data(usuario)
                .build());
    }
}
