package com.reservas.service;

import com.reservas.dto.request.ClienteRequest;
import com.reservas.dto.response.ClienteResponse;
import com.reservas.entity.Cliente;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.NotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.CitaRepository;
import com.reservas.repository.ClienteRepository;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - Pruebas Unitarias")
class ClienteServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlanLimitesService planLimitesService;

    @InjectMocks
    private ClienteService clienteService;

    private Usuario usuarioMock;
    private Negocio negocioMock;
    private Cliente clienteMock;
    private ClienteRequest clienteRequestMock;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .email("negocio@test.com")
                .nombre("Salon de Belleza Test")
                .tipo("salon")
                .estadoPago("activo")
                .plan("profesional")
                .fechaInicioPlan(LocalDateTime.now())
                .build();

        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario@test.com")
                .passwordHash("$2a$10$hashedPassword")
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("García")
                .rol("admin")
                .activo(true)
                .negocio(negocioMock)
                .build();

        clienteMock = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("María")
                .apellidoPaterno("González")
                .apellidoMaterno("López")
                .email("maria@cliente.com")
                .telefono("1234567890")
                .notas("Cliente frecuente")
                .negocio(negocioMock)
                .build();

        clienteRequestMock = ClienteRequest.builder()
                .nombre("María")
                .apellidoPaterno("González")
                .apellidoMaterno("López")
                .email("maria@cliente.com")
                .telefono("1234567890")
                .notas("Cliente frecuente")
                .build();
    }

    @Test
    @DisplayName("Crear cliente - Exitoso")
    void testCrearCliente_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findByNegocioAndEmail(any(Negocio.class), anyString()))
                .thenReturn(Optional.empty());
        doNothing().when(planLimitesService).validarLimiteClientes(any(UUID.class), any());
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteMock);

        // Act
        ClienteResponse response = clienteService.crearCliente("usuario@test.com", clienteRequestMock);

        // Assert
        assertNotNull(response);
        assertEquals(clienteMock.getId(), response.getId());
        assertEquals("María", response.getNombre());
        assertEquals("González", response.getApellidoPaterno());
        assertEquals("López", response.getApellidoMaterno());
        assertEquals("maria@cliente.com", response.getEmail());
        assertEquals("María González López", response.getNombreCompleto());

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(clienteRepository, times(1)).findByNegocioAndEmail(any(Negocio.class), anyString());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Crear cliente - Usuario no encontrado")
    void testCrearCliente_UsuarioNoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            clienteService.crearCliente("usuario@inexistente.com", clienteRequestMock);
        });

        verify(usuarioRepository, times(1)).findByEmail(anyString());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Crear cliente - Email duplicado")
    void testCrearCliente_EmailDuplicado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findByNegocioAndEmail(any(Negocio.class), anyString()))
                .thenReturn(Optional.of(clienteMock));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            clienteService.crearCliente("usuario@test.com", clienteRequestMock);
        });

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Listar clientes - Sin búsqueda")
    void testListarClientes_SinBusqueda() {
        // Arrange
        Cliente cliente1 = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("María")
                .apellidoPaterno("González")
                .negocio(negocioMock)
                .build();

        Cliente cliente2 = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("Pedro")
                .apellidoPaterno("Martínez")
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findByNegocio(any(Negocio.class)))
                .thenReturn(Arrays.asList(cliente1, cliente2));

        // Act
        List<ClienteResponse> response = clienteService.listarClientes("usuario@test.com", null);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertNotNull(response.get(0).getId());
        assertNotNull(response.get(1).getId());

        verify(clienteRepository, times(1)).findByNegocio(any(Negocio.class));
        verify(clienteRepository, never()).searchClientes(any(Negocio.class), anyString());
    }

    @Test
    @DisplayName("Listar clientes - Con búsqueda")
    void testListarClientes_ConBusqueda() {
        // Arrange
        Cliente clienteEncontrado = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("María")
                .apellidoPaterno("González")
                .negocio(negocioMock)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.searchClientes(any(Negocio.class), anyString()))
                .thenReturn(Arrays.asList(clienteEncontrado));

        // Act
        List<ClienteResponse> response = clienteService.listarClientes("usuario@test.com", "María");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());

        verify(clienteRepository, times(1)).searchClientes(any(Negocio.class), anyString());
        verify(clienteRepository, never()).findByNegocio(any(Negocio.class));
    }

    @Test
    @DisplayName("Obtener cliente - Exitoso")
    void testObtenerCliente_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));

        // Act
        ClienteResponse response = clienteService.obtenerCliente("usuario@test.com", clienteMock.getId().toString());

        // Assert
        assertNotNull(response);
        assertEquals(clienteMock.getId(), response.getId());
        assertEquals("María", response.getNombre());

        verify(clienteRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Obtener cliente - No autorizado")
    void testObtenerCliente_NoAutorizado() {
        // Arrange
        Negocio otroNegocio = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Otro Negocio")
                .build();

        Cliente clienteOtroNegocio = Cliente.builder()
                .id(UUID.randomUUID())
                .nombre("Cliente de otro negocio")
                .apellidoPaterno("Test")
                .negocio(otroNegocio)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteOtroNegocio));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            clienteService.obtenerCliente("usuario@test.com", clienteOtroNegocio.getId().toString());
        });
    }

    @Test
    @DisplayName("Actualizar cliente - Exitoso")
    void testActualizarCliente_Exitoso() {
        // Arrange
        ClienteRequest updateRequest = ClienteRequest.builder()
                .nombre("María Actualizada")
                .apellidoPaterno("González")
                .apellidoMaterno("López")
                .email("maria.nueva@cliente.com")
                .telefono("9876543210")
                .notas("Notas actualizadas")
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.findByNegocioAndEmail(any(Negocio.class), anyString()))
                .thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteMock);

        // Act
        ClienteResponse response = clienteService.actualizarCliente("usuario@test.com", clienteMock.getId().toString(), updateRequest);

        // Assert
        assertNotNull(response);
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Actualizar cliente - Email duplicado")
    void testActualizarCliente_EmailDuplicado() {
        // Arrange
        Cliente otroCliente = Cliente.builder()
                .id(UUID.randomUUID())
                .email("email-existente@test.com")
                .negocio(negocioMock)
                .build();

        ClienteRequest updateRequest = ClienteRequest.builder()
                .nombre("María")
                .apellidoPaterno("González")
                .email("email-existente@test.com")
                .telefono("1234567890")
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.findByNegocioAndEmail(any(Negocio.class), anyString()))
                .thenReturn(Optional.of(otroCliente));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            clienteService.actualizarCliente("usuario@test.com", clienteMock.getId().toString(), updateRequest);
        });

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Eliminar cliente - Exitoso")
    void testEliminarCliente_Exitoso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteMock));
        doNothing().when(clienteRepository).delete(any(Cliente.class));

        // Act
        clienteService.eliminarCliente("usuario@test.com", clienteMock.getId().toString());

        // Assert
        verify(clienteRepository, times(1)).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("Eliminar cliente - Cliente no encontrado")
    void testEliminarCliente_ClienteNoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioMock));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        String fakeUUID = UUID.randomUUID().toString();

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            clienteService.eliminarCliente("usuario@test.com", fakeUUID);
        });

        verify(clienteRepository, never()).delete(any(Cliente.class));
    }
}
