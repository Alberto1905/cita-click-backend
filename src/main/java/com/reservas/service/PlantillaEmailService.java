package com.reservas.service;

import com.reservas.dto.request.PlantillaEmailConfigRequest;
import com.reservas.dto.response.PlantillaEmailConfigResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.PlantillaEmailConfig;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.PlantillaEmailConfigRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestionar plantillas de email personalizadas
 */
@Service
@Slf4j
public class PlantillaEmailService {

    @Autowired
    private PlantillaEmailConfigRepository plantillaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtiene la configuración de plantilla de email del negocio del usuario
     * Si no existe, retorna una configuración por defecto
     */
    @Transactional(readOnly = true)
    public PlantillaEmailConfigResponse obtenerConfiguracion(String email) {
        log.info("Obteniendo configuración de plantilla para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        return plantillaRepository.findByNegocio(negocio)
                .map(PlantillaEmailConfigResponse::fromEntity)
                .orElseGet(() -> crearConfiguracionPorDefecto(negocio));
    }

    /**
     * Crea o actualiza la configuración de plantilla de email
     */
    @Transactional
    public PlantillaEmailConfigResponse guardarConfiguracion(String email, PlantillaEmailConfigRequest request) {
        log.info("Guardando configuración de plantilla para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        PlantillaEmailConfig config = plantillaRepository.findByNegocio(negocio)
                .orElse(PlantillaEmailConfig.builder()
                        .negocio(negocio)
                        .build());

        // Actualizar campos
        if (request.getLogoUrl() != null) {
            config.setLogoUrl(request.getLogoUrl());
        }
        if (request.getColorPrimario() != null) {
            config.setColorPrimario(request.getColorPrimario());
        }
        if (request.getColorSecundario() != null) {
            config.setColorSecundario(request.getColorSecundario());
        }
        if (request.getColorFondo() != null) {
            config.setColorFondo(request.getColorFondo());
        }
        if (request.getMensajeBienvenida() != null) {
            config.setMensajeBienvenida(request.getMensajeBienvenida());
        }
        if (request.getFirma() != null) {
            config.setFirma(request.getFirma());
        }
        if (request.getInfoContacto() != null) {
            config.setInfoContacto(request.getInfoContacto());
        }
        if (request.getDisenoBase() != null) {
            config.setDisenoBase(request.getDisenoBase());
        }
        if (request.getActiva() != null) {
            config.setActiva(request.getActiva());
        }

        config = plantillaRepository.save(config);
        log.info("✅ Configuración de plantilla guardada para negocio: {}", negocio.getId());

        return PlantillaEmailConfigResponse.fromEntity(config);
    }

    /**
     * Elimina el logo de la plantilla
     */
    @Transactional
    public PlantillaEmailConfigResponse eliminarLogo(String email) {
        log.info("Eliminando logo de plantilla para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        PlantillaEmailConfig config = plantillaRepository.findByNegocio(negocio)
                .orElseThrow(() -> new NotFoundException("Configuración de plantilla no encontrada"));

        config.setLogoUrl(null);
        config = plantillaRepository.save(config);

        log.info("✅ Logo eliminado de plantilla para negocio: {}", negocio.getId());

        return PlantillaEmailConfigResponse.fromEntity(config);
    }

    /**
     * Restaura la configuración a valores por defecto
     */
    @Transactional
    public PlantillaEmailConfigResponse restaurarPorDefecto(String email) {
        log.info("Restaurando configuración por defecto para usuario: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Negocio negocio = usuario.getNegocio();
        if (negocio == null) {
            throw new NotFoundException("Negocio no encontrado");
        }

        // Eliminar configuración existente si hay
        plantillaRepository.findByNegocio(negocio)
                .ifPresent(plantillaRepository::delete);

        // Crear nueva configuración por defecto
        PlantillaEmailConfig config = PlantillaEmailConfig.builder()
                .negocio(negocio)
                .colorPrimario("#1E40AF")
                .colorSecundario("#3B82F6")
                .colorFondo("#F3F4F6")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.CLASICO)
                .activa(true)
                .build();

        config = plantillaRepository.save(config);
        log.info("✅ Configuración restaurada a valores por defecto para negocio: {}", negocio.getId());

        return PlantillaEmailConfigResponse.fromEntity(config);
    }

    /**
     * Crea una respuesta con configuración por defecto sin guardar en BD
     */
    private PlantillaEmailConfigResponse crearConfiguracionPorDefecto(Negocio negocio) {
        return PlantillaEmailConfigResponse.builder()
                .negocioId(negocio.getId().toString())
                .colorPrimario("#1E40AF")
                .colorSecundario("#3B82F6")
                .colorFondo("#F3F4F6")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.CLASICO)
                .activa(true)
                .build();
    }

    /**
     * Obtiene la configuración de plantilla por negocio (uso interno)
     * Usado por EmailService para generar HTML personalizado
     */
    @Transactional(readOnly = true)
    public PlantillaEmailConfig obtenerConfiguracionPorNegocio(Negocio negocio) {
        return plantillaRepository.findByNegocio(negocio)
                .orElse(null);
    }
}
