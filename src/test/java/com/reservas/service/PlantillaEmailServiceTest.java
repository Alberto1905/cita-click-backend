package com.reservas.service;

import com.reservas.dto.request.PlantillaEmailConfigRequest;
import com.reservas.dto.response.PlantillaEmailConfigResponse;
import com.reservas.entity.Negocio;
import com.reservas.entity.PlantillaEmailConfig;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotFoundException;
import com.reservas.repository.PlantillaEmailConfigRepository;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PlantillaEmailService
 */
@ExtendWith(MockitoExtension.class)
class PlantillaEmailServiceTest {

    @InjectMocks
    private PlantillaEmailService plantillaEmailService;

    @Mock
    private PlantillaEmailConfigRepository plantillaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void testObtenerConfiguracion_ConfiguracionExistente() {
        // Given
        String email = "admin@barberia.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Barbería El Clásico")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        PlantillaEmailConfig config = PlantillaEmailConfig.builder()
                .id(UUID.randomUUID().toString())
                .negocio(negocio)
                .colorPrimario("#1E40AF")
                .colorSecundario("#3B82F6")
                .colorFondo("#F3F4F6")
                .mensajeBienvenida("¡Bienvenido a nuestra barbería!")
                .firma("Equipo de Barbería El Clásico")
                .infoContacto("Tel: 33 1234 5678")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.CLASICO)
                .activa(true)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.of(config));

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.obtenerConfiguracion(email);

        // Then
        assertNotNull(response);
        assertEquals(config.getId(), response.getId());
        assertEquals(negocio.getId().toString(), response.getNegocioId());
        assertEquals("#1E40AF", response.getColorPrimario());
        assertEquals("#3B82F6", response.getColorSecundario());
        assertEquals("#F3F4F6", response.getColorFondo());
        assertTrue(response.isActiva());

        verify(usuarioRepository).findByEmail(email);
        verify(plantillaRepository).findByNegocio(negocio);
    }

    @Test
    void testObtenerConfiguracion_ConfiguracionNoExiste_RetornaPorDefecto() {
        // Given
        String email = "admin@spa.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Spa Bienestar")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.empty());

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.obtenerConfiguracion(email);

        // Then
        assertNotNull(response);
        assertEquals(negocio.getId().toString(), response.getNegocioId());
        assertEquals("#1E40AF", response.getColorPrimario());
        assertEquals("#3B82F6", response.getColorSecundario());
        assertEquals("#F3F4F6", response.getColorFondo());
        assertEquals(PlantillaEmailConfig.TipoDiseno.CLASICO, response.getDisenoBase());
        assertTrue(response.isActiva());

        verify(usuarioRepository).findByEmail(email);
        verify(plantillaRepository).findByNegocio(negocio);
    }

    @Test
    void testObtenerConfiguracion_UsuarioNoExiste_LanzaExcepcion() {
        // Given
        String email = "noexiste@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            plantillaEmailService.obtenerConfiguracion(email);
        });

        verify(usuarioRepository).findByEmail(email);
        verify(plantillaRepository, never()).findByNegocio(any());
    }

    @Test
    void testObtenerConfiguracion_UsuarioSinNegocio_LanzaExcepcion() {
        // Given
        String email = "admin@test.com";
        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(null)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            plantillaEmailService.obtenerConfiguracion(email);
        });

        verify(usuarioRepository).findByEmail(email);
    }

    @Test
    void testGuardarConfiguracion_ConfiguracionNueva() {
        // Given
        String email = "admin@salon.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Salón Belleza Total")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        PlantillaEmailConfigRequest request = PlantillaEmailConfigRequest.builder()
                .colorPrimario("#FF5733")
                .colorSecundario("#FFC300")
                .colorFondo("#FFFFFF")
                .mensajeBienvenida("¡Hola! Bienvenido a nuestro salón")
                .firma("Equipo de Salón Belleza Total")
                .infoContacto("Calle Principal 123")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.MODERNO)
                .activa(true)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.empty());
        when(plantillaRepository.save(any(PlantillaEmailConfig.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.guardarConfiguracion(email, request);

        // Then
        assertNotNull(response);
        assertEquals(negocio.getId().toString(), response.getNegocioId());
        assertEquals("#FF5733", response.getColorPrimario());
        assertEquals("#FFC300", response.getColorSecundario());
        assertEquals("#FFFFFF", response.getColorFondo());
        assertEquals("¡Hola! Bienvenido a nuestro salón", response.getMensajeBienvenida());
        assertEquals(PlantillaEmailConfig.TipoDiseno.MODERNO, response.getDisenoBase());
        assertTrue(response.isActiva());

        verify(usuarioRepository).findByEmail(email);
        verify(plantillaRepository).findByNegocio(negocio);
        verify(plantillaRepository).save(any(PlantillaEmailConfig.class));
    }

    @Test
    void testGuardarConfiguracion_ConfiguracionExistente_Actualiza() {
        // Given
        String email = "admin@gym.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Gimnasio PowerFit")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        PlantillaEmailConfig configExistente = PlantillaEmailConfig.builder()
                .id(UUID.randomUUID().toString())
                .negocio(negocio)
                .colorPrimario("#000000")
                .colorSecundario("#CCCCCC")
                .colorFondo("#FFFFFF")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.CLASICO)
                .activa(true)
                .build();

        PlantillaEmailConfigRequest request = PlantillaEmailConfigRequest.builder()
                .colorPrimario("#FF0000")
                .colorSecundario("#00FF00")
                .activa(false)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.of(configExistente));
        when(plantillaRepository.save(any(PlantillaEmailConfig.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.guardarConfiguracion(email, request);

        // Then
        assertNotNull(response);
        assertEquals("#FF0000", response.getColorPrimario());
        assertEquals("#00FF00", response.getColorSecundario());
        assertFalse(response.isActiva());

        verify(plantillaRepository).save(any(PlantillaEmailConfig.class));
    }

    @Test
    void testGuardarConfiguracion_ActualizacionParcial() {
        // Given
        String email = "admin@clinica.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Clínica Dental")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        PlantillaEmailConfig configExistente = PlantillaEmailConfig.builder()
                .id(UUID.randomUUID().toString())
                .negocio(negocio)
                .colorPrimario("#000000")
                .colorSecundario("#CCCCCC")
                .colorFondo("#FFFFFF")
                .mensajeBienvenida("Mensaje anterior")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.CLASICO)
                .activa(true)
                .build();

        // Solo actualizar el mensaje de bienvenida
        PlantillaEmailConfigRequest request = PlantillaEmailConfigRequest.builder()
                .mensajeBienvenida("Mensaje actualizado")
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.of(configExistente));
        when(plantillaRepository.save(any(PlantillaEmailConfig.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.guardarConfiguracion(email, request);

        // Then
        assertNotNull(response);
        assertEquals("Mensaje actualizado", response.getMensajeBienvenida());
        // Los demás campos deben mantener sus valores anteriores
        assertEquals("#000000", response.getColorPrimario());
        assertEquals("#CCCCCC", response.getColorSecundario());
        assertEquals("#FFFFFF", response.getColorFondo());

        verify(plantillaRepository).save(any(PlantillaEmailConfig.class));
    }

    @Test
    void testRestaurarPorDefecto_Exitoso() {
        // Given
        String email = "admin@veterinaria.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Clínica Veterinaria")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        PlantillaEmailConfig configExistente = PlantillaEmailConfig.builder()
                .id(UUID.randomUUID().toString())
                .negocio(negocio)
                .colorPrimario("#FF0000")
                .colorSecundario("#00FF00")
                .colorFondo("#0000FF")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.MODERNO)
                .activa(false)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.of(configExistente));
        when(plantillaRepository.save(any(PlantillaEmailConfig.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.restaurarPorDefecto(email);

        // Then
        assertNotNull(response);
        assertEquals(negocio.getId().toString(), response.getNegocioId());
        assertEquals("#1E40AF", response.getColorPrimario());
        assertEquals("#3B82F6", response.getColorSecundario());
        assertEquals("#F3F4F6", response.getColorFondo());
        assertEquals(PlantillaEmailConfig.TipoDiseno.CLASICO, response.getDisenoBase());
        assertTrue(response.isActiva());

        verify(plantillaRepository).findByNegocio(negocio);
        verify(plantillaRepository).delete(configExistente);
        verify(plantillaRepository).save(any(PlantillaEmailConfig.class));
    }

    @Test
    void testRestaurarPorDefecto_SinConfiguracionPrevia() {
        // Given
        String email = "admin@consultorio.com";
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Consultorio Médico")
                .build();

        Usuario usuario = Usuario.builder()
                .email(email)
                .negocio(negocio)
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.empty());
        when(plantillaRepository.save(any(PlantillaEmailConfig.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PlantillaEmailConfigResponse response = plantillaEmailService.restaurarPorDefecto(email);

        // Then
        assertNotNull(response);
        assertEquals("#1E40AF", response.getColorPrimario());
        assertEquals("#3B82F6", response.getColorSecundario());
        assertEquals("#F3F4F6", response.getColorFondo());
        assertTrue(response.isActiva());

        verify(plantillaRepository, never()).delete(any());
        verify(plantillaRepository).save(any(PlantillaEmailConfig.class));
    }

    @Test
    void testObtenerConfiguracionPorNegocio_ConfiguracionExiste() {
        // Given
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Centro de Masajes")
                .build();

        PlantillaEmailConfig config = PlantillaEmailConfig.builder()
                .id(UUID.randomUUID().toString())
                .negocio(negocio)
                .colorPrimario("#1E40AF")
                .colorSecundario("#3B82F6")
                .colorFondo("#F3F4F6")
                .disenoBase(PlantillaEmailConfig.TipoDiseno.MINIMALISTA)
                .activa(true)
                .build();

        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.of(config));

        // When
        PlantillaEmailConfig result = plantillaEmailService.obtenerConfiguracionPorNegocio(negocio);

        // Then
        assertNotNull(result);
        assertEquals(config.getId(), result.getId());
        assertEquals(PlantillaEmailConfig.TipoDiseno.MINIMALISTA, result.getDisenoBase());

        verify(plantillaRepository).findByNegocio(negocio);
    }

    @Test
    void testObtenerConfiguracionPorNegocio_ConfiguracionNoExiste() {
        // Given
        Negocio negocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Negocio Sin Config")
                .build();

        when(plantillaRepository.findByNegocio(negocio)).thenReturn(Optional.empty());

        // When
        PlantillaEmailConfig result = plantillaEmailService.obtenerConfiguracionPorNegocio(negocio);

        // Then
        assertNull(result);

        verify(plantillaRepository).findByNegocio(negocio);
    }
}
