package com.reservas.service;

import com.reservas.dto.request.ClienteRequest;
import com.reservas.dto.response.ClienteResponse;
import com.reservas.entity.Cliente;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.ClienteRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PlanLimitesService planLimitesService;

    @Transactional
    public ClienteResponse crearCliente(String email, ClienteRequest request) {
        log.info("Creando cliente para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // VALIDAR LÍMITE DE CLIENTES
        com.reservas.entity.enums.TipoPlan plan = com.reservas.entity.enums.TipoPlan.fromCodigo(negocio.getPlan());
        planLimitesService.validarLimiteClientes(negocio.getId(), plan);

        // Verificar si ya existe cliente con el mismo email en este negocio
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            clienteRepository.findByNegocioAndEmail(negocio, request.getEmail())
                    .ifPresent(c -> {
                        throw new BadRequestException("Ya existe un cliente con este email en tu negocio");
                    });
        }

        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .fechaNacimiento(request.getFechaNacimiento())
                .genero(request.getGenero())
                .notas(request.getNotas())
                .negocio(negocio)
                .build();

        cliente = clienteRepository.save(cliente);
        log.info(" Cliente creado: {} - {} {}", cliente.getId(), cliente.getNombre(), cliente.getApellidoPaterno());

        // ACTUALIZAR USO
        planLimitesService.actualizarUso(negocio.getId());

        return mapToResponse(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientes(String email, String search) {
        log.info("Listando clientes para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        List<Cliente> clientes;
        if (search != null && !search.isBlank()) {
            clientes = clienteRepository.searchClientes(negocio, search);
            log.info("Clientes encontrados con búsqueda '{}': {}", search, clientes.size());
        } else {
            clientes = clienteRepository.findByNegocio(negocio);
            log.info("Todos los clientes obtenidos: {}", clientes.size());
        }

        return clientes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(String email, String clienteId) {
        log.info("Obteniendo cliente: {} para usuario: {}", clienteId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findById(UUID.fromString(clienteId))
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        // Verificar que el cliente pertenece al negocio del usuario
        if (!cliente.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para acceder a este cliente");
        }

        return mapToResponse(cliente);
    }

    @Transactional
    public ClienteResponse actualizarCliente(String email, String clienteId, ClienteRequest request) {
        log.info("Actualizando cliente: {} para usuario: {}", clienteId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findById(UUID.fromString(clienteId))
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        // Verificar que el cliente pertenece al negocio del usuario
        if (!cliente.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para actualizar este cliente");
        }

        // Verificar email único si se está cambiando
        if (request.getEmail() != null && !request.getEmail().equals(cliente.getEmail())) {
            clienteRepository.findByNegocioAndEmail(cliente.getNegocio(), request.getEmail())
                    .ifPresent(c -> {
                        throw new BadRequestException("Ya existe otro cliente con este email");
                    });
        }

        cliente.setNombre(request.getNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setEmail(request.getEmail());
        cliente.setTelefono(request.getTelefono());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setGenero(request.getGenero());
        cliente.setNotas(request.getNotas());

        cliente = clienteRepository.save(cliente);
        log.info(" Cliente actualizado: {}", clienteId);

        return mapToResponse(cliente);
    }

    @Transactional
    public void eliminarCliente(String email, String clienteId) {
        log.info("Eliminando cliente: {} para usuario: {}", clienteId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findById(UUID.fromString(clienteId))
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        // Verificar que el cliente pertenece al negocio del usuario
        if (!cliente.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para eliminar este cliente");
        }

        // TODO: Verificar que no tenga citas pendientes antes de eliminar
        // Por ahora solo eliminamos
        clienteRepository.delete(cliente);
        log.info(" Cliente eliminado: {}", clienteId);
    }

    private ClienteResponse mapToResponse(Cliente cliente) {
        String nombreCompleto = cliente.getNombre() + " " + cliente.getApellidoPaterno();
        if (cliente.getApellidoMaterno() != null && !cliente.getApellidoMaterno().isBlank()) {
            nombreCompleto += " " + cliente.getApellidoMaterno();
        }

        return ClienteResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .nombreCompleto(nombreCompleto)
                .email(cliente.getEmail())
                .telefono(cliente.getTelefono())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .genero(cliente.getGenero())
                .notas(cliente.getNotas())
                .totalCitas(0) // TODO: Calcular del repositorio
                .build();
    }
}
