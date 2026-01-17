package com.reservas.service;

import com.reservas.dto.request.ServicioRequest;
import com.reservas.dto.response.ServicioResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.ServicioRepository;
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
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private PlanLimitesService planLimitesService;

    @Transactional
    public ServicioResponse crearServicio(String email, ServicioRequest request) {
        log.info("Creando servicio para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado para el usuario");
        }

        // VALIDAR LÍMITE DE SERVICIOS
        com.reservas.entity.enums.TipoPlan plan = com.reservas.entity.enums.TipoPlan.fromCodigo(negocio.getPlan());
        planLimitesService.validarLimiteServicios(negocio.getId(), plan);

        Servicio servicio = Servicio.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .duracionMinutos(request.getDuracionMinutos())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .negocio(negocio)
                .build();

        servicio = servicioRepository.save(servicio);
        log.info(" Servicio creado: {} - {}", servicio.getId(), servicio.getNombre());

        // ACTUALIZAR USO
        planLimitesService.actualizarUso(negocio.getId());

        return mapToResponse(servicio);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listarServicios(String email, Boolean soloActivos) {
        log.info("Listando servicios para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        List<Servicio> servicios;
        if (soloActivos != null && soloActivos) {
            servicios = servicioRepository.findByNegocioAndActivoTrue(negocio);
            log.info("Servicios activos obtenidos: {}", servicios.size());
        } else {
            servicios = servicioRepository.findByNegocio(negocio);
            log.info("Todos los servicios obtenidos: {}", servicios.size());
        }

        return servicios.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtenerServicio(String email, String servicioId) {
        log.info("Obteniendo servicio: {} para usuario: {}", servicioId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Servicio servicio = servicioRepository.findById(UUID.fromString(servicioId))
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        // Verificar que el servicio pertenece al negocio del usuario
        if (!servicio.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para acceder a este servicio");
        }

        return mapToResponse(servicio);
    }

    @Transactional
    public ServicioResponse actualizarServicio(String email, String servicioId, ServicioRequest request) {
        log.info("Actualizando servicio: {} para usuario: {}", servicioId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Servicio servicio = servicioRepository.findById(UUID.fromString(servicioId))
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        // Verificar que el servicio pertenece al negocio del usuario
        if (!servicio.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para actualizar este servicio");
        }

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setDuracionMinutos(request.getDuracionMinutos());

        if (request.getActivo() != null) {
            servicio.setActivo(request.getActivo());
        }

        servicio = servicioRepository.save(servicio);
        log.info(" Servicio actualizado: {}", servicioId);

        return mapToResponse(servicio);
    }

    @Transactional
    public void eliminarServicio(String email, String servicioId) {
        log.info("Eliminando servicio: {} para usuario: {}", servicioId, email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Servicio servicio = servicioRepository.findById(UUID.fromString(servicioId))
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        // Verificar que el servicio pertenece al negocio del usuario
        if (!servicio.getNegocio().getId().equals(usuario.getNegocio().getId())) {
            throw new UnauthorizedException("No tienes permiso para eliminar este servicio");
        }

        // Soft delete - solo marcar como inactivo
        servicio.setActivo(false);
        servicioRepository.save(servicio);
        log.info(" Servicio marcado como inactivo: {}", servicioId);
    }

    private ServicioResponse mapToResponse(Servicio servicio) {
        return ServicioResponse.builder()
                .id(servicio.getId())
                .nombre(servicio.getNombre())
                .descripcion(servicio.getDescripcion())
                .precio(servicio.getPrecio())
                .duracionMinutos(servicio.getDuracionMinutos())
                .activo(servicio.isActivo())
                .build();
    }
}
