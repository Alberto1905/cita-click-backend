package com.reservas.controller;

import com.reservas.dto.request.ClienteRequest;
import com.reservas.dto.response.ApiResponse;
import com.reservas.dto.response.ClienteResponse;
import com.reservas.dto.response.ClientePerfil360Response;
import com.reservas.service.ClienteService;
import com.reservas.service.ClientePerfil360Service;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Slf4j
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClientePerfil360Service clientePerfil360Service;

    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> crearCliente(
            @Valid @RequestBody ClienteRequest request, Authentication auth) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ClienteResponse>builder()
                    .success(true).message("Cliente creado exitosamente")
                    .data(clienteService.crearCliente(auth.getName(), request)).build());
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ClienteResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listarClientes(
            @RequestParam(required = false) String search, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<List<ClienteResponse>>builder()
                    .success(true).message("Clientes obtenidos exitosamente")
                    .data(clienteService.listarClientes(auth.getName(), search)).build());
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<List<ClienteResponse>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerCliente(@PathVariable String id, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<ClienteResponse>builder()
                    .success(true).message("Cliente obtenido exitosamente")
                    .data(clienteService.obtenerCliente(auth.getName(), id)).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<ClienteResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(
            @PathVariable String id, @Valid @RequestBody ClienteRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.<ClienteResponse>builder()
                    .success(true).message("Cliente actualizado exitosamente")
                    .data(clienteService.actualizarCliente(auth.getName(), id, request)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<ClienteResponse>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarCliente(@PathVariable String id, Authentication auth) {
        try {
            clienteService.eliminarCliente(auth.getName(), id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Cliente eliminado exitosamente").build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    /**
     * Endpoint para obtener el perfil 360 del cliente
     */
    @GetMapping("/{id}/perfil360")
    public ResponseEntity<ApiResponse<ClientePerfil360Response>> obtenerPerfil360(
            @PathVariable String id, Authentication auth) {
        try {
            log.info("[Perfil 360] Solicitando perfil 360 para cliente: {}", id);
            ClientePerfil360Response perfil = clientePerfil360Service.obtenerPerfil360(auth.getName(), id);
            return ResponseEntity.ok(ApiResponse.<ClientePerfil360Response>builder()
                    .success(true)
                    .message("Perfil 360 obtenido exitosamente")
                    .data(perfil)
                    .build());
        } catch (Exception e) {
            log.error("[Perfil 360] Error al obtener perfil: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ClientePerfil360Response>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
