package com.reservas.service;

import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.entity.enums.UsuarioRol;
import com.reservas.exception.PermisoInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisosService - Pruebas Unitarias")
class PermisosServiceTest {

    @InjectMocks
    private PermisosService permisosService;

    private Negocio negocioMock;
    private Usuario usuarioOwner;
    private Usuario usuarioAdmin;
    private Usuario usuarioEmpleado;
    private Usuario usuarioRecepcionista;

    @BeforeEach
    void setUp() {
        negocioMock = Negocio.builder()
                .id(UUID.randomUUID())
                .nombre("Salon Test")
                .email("salon@test.com")
                .tipo("salon")
                .plan("profesional")
                .estadoPago("activo")
                .build();

        usuarioOwner = Usuario.builder()
                .id(UUID.randomUUID())
                .email("owner@test.com")
                .nombre("Owner")
                .apellidoPaterno("Test")
                .rol("owner")
                .negocio(negocioMock)
                .build();

        usuarioAdmin = Usuario.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .nombre("Admin")
                .apellidoPaterno("Test")
                .rol("admin")
                .negocio(negocioMock)
                .build();

        usuarioEmpleado = Usuario.builder()
                .id(UUID.randomUUID())
                .email("empleado@test.com")
                .nombre("Empleado")
                .apellidoPaterno("Test")
                .rol("empleado")
                .negocio(negocioMock)
                .build();

        usuarioRecepcionista = Usuario.builder()
                .id(UUID.randomUUID())
                .email("recepcionista@test.com")
                .nombre("Recepcionista")
                .apellidoPaterno("Test")
                .rol("recepcionista")
                .negocio(negocioMock)
                .build();
    }

    // Tests para permisos de OWNER
    @Test
    @DisplayName("OWNER debe tener permiso para gestionar usuarios")
    void ownerDebeTenerPermisoGestionarUsuarios() {
        assertTrue(permisosService.tienePermiso(usuarioOwner, "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("OWNER debe tener permiso para cambiar plan")
    void ownerDebeTenerPermisoCambiarPlan() {
        assertTrue(permisosService.tienePermiso(usuarioOwner, "CAMBIAR_PLAN"));
    }

    @Test
    @DisplayName("OWNER debe tener todos los permisos de administración")
    void ownerDebeTenerTodosLosPermisosAdministracion() {
        assertTrue(permisosService.tienePermiso(usuarioOwner, "GESTIONAR_USUARIOS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "INVITAR_USUARIOS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "CAMBIAR_ROL_USUARIOS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "DESACTIVAR_USUARIOS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "VER_REPORTES"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "CONFIGURAR_NEGOCIO"));
    }

    @Test
    @DisplayName("OWNER debe tener permisos operativos")
    void ownerDebeTenerPermisosOperativos() {
        assertTrue(permisosService.tienePermiso(usuarioOwner, "CREAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "CANCELAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "MODIFICAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "GESTIONAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioOwner, "GESTIONAR_SERVICIOS"));
    }

    // Tests para permisos de ADMIN
    @Test
    @DisplayName("ADMIN debe tener permiso para gestionar usuarios")
    void adminDebeTenerPermisoGestionarUsuarios() {
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("ADMIN NO debe tener permiso para cambiar plan")
    void adminNoDebeTenerPermisoCambiarPlan() {
        assertFalse(permisosService.tienePermiso(usuarioAdmin, "CAMBIAR_PLAN"));
    }

    @Test
    @DisplayName("ADMIN debe tener permisos de reportes y configuración")
    void adminDebeTenerPermisosReportesConfiguracion() {
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "VER_REPORTES"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "DESCARGAR_REPORTES"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "CONFIGURAR_NEGOCIO"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "VER_METRICAS"));
    }

    @Test
    @DisplayName("ADMIN debe tener permisos de gestión de clientes y servicios")
    void adminDebeTenerPermisosGestionClientesServicios() {
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "GESTIONAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "CREAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "MODIFICAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "ELIMINAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "GESTIONAR_SERVICIOS"));
        assertTrue(permisosService.tienePermiso(usuarioAdmin, "ELIMINAR_SERVICIOS"));
    }

    // Tests para permisos de EMPLEADO
    @Test
    @DisplayName("EMPLEADO debe tener permisos operativos básicos")
    void empleadoDebeTenerPermisosOperativosBasicos() {
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "CREAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "CANCELAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "MODIFICAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "GESTIONAR_CLIENTES"));
    }

    @Test
    @DisplayName("EMPLEADO debe poder ver reportes y métricas")
    void empleadoDebePodedVerReportesMetricas() {
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "VER_REPORTES"));
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "VER_METRICAS"));
        assertTrue(permisosService.tienePermiso(usuarioEmpleado, "VER_DASHBOARD"));
    }

    @Test
    @DisplayName("EMPLEADO NO debe poder gestionar usuarios")
    void empleadoNoPuedeGestionarUsuarios() {
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "GESTIONAR_USUARIOS"));
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "INVITAR_USUARIOS"));
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "CAMBIAR_ROL_USUARIOS"));
    }

    @Test
    @DisplayName("EMPLEADO NO debe poder eliminar clientes o servicios")
    void empleadoNoPuedeEliminar() {
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "ELIMINAR_CLIENTES"));
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "ELIMINAR_SERVICIOS"));
    }

    @Test
    @DisplayName("EMPLEADO NO debe poder configurar negocio o cambiar plan")
    void empleadoNoPuedeConfigurarNegocio() {
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "CONFIGURAR_NEGOCIO"));
        assertFalse(permisosService.tienePermiso(usuarioEmpleado, "CAMBIAR_PLAN"));
    }

    // Tests para permisos de RECEPCIONISTA
    @Test
    @DisplayName("RECEPCIONISTA debe tener permisos básicos de citas")
    void recepcionistaDebeTenerPermisosBasicosCitas() {
        assertTrue(permisosService.tienePermiso(usuarioRecepcionista, "CREAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioRecepcionista, "CANCELAR_CITAS"));
        assertTrue(permisosService.tienePermiso(usuarioRecepcionista, "MODIFICAR_CITAS"));
    }

    @Test
    @DisplayName("RECEPCIONISTA debe poder gestionar clientes básicamente")
    void recepcionistaDebeGestionarClientesBasicamente() {
        assertTrue(permisosService.tienePermiso(usuarioRecepcionista, "GESTIONAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioRecepcionista, "CREAR_CLIENTES"));
        assertTrue(permisosService.tienePermiso(usuarioRecepcionista, "MODIFICAR_CLIENTES"));
    }

    @Test
    @DisplayName("RECEPCIONISTA NO debe poder ver reportes o métricas")
    void recepcionistaNoPuedeVerReportesMetricas() {
        assertFalse(permisosService.tienePermiso(usuarioRecepcionista, "VER_REPORTES"));
        assertFalse(permisosService.tienePermiso(usuarioRecepcionista, "VER_METRICAS"));
    }

    @Test
    @DisplayName("RECEPCIONISTA NO debe poder eliminar clientes")
    void recepcionistaNoPuedeEliminarClientes() {
        assertFalse(permisosService.tienePermiso(usuarioRecepcionista, "ELIMINAR_CLIENTES"));
    }

    @Test
    @DisplayName("RECEPCIONISTA NO debe poder gestionar servicios")
    void recepcionistaNoPuedeGestionarServicios() {
        assertFalse(permisosService.tienePermiso(usuarioRecepcionista, "GESTIONAR_SERVICIOS"));
        assertFalse(permisosService.tienePermiso(usuarioRecepcionista, "CREAR_SERVICIOS"));
        assertFalse(permisosService.tienePermiso(usuarioRecepcionista, "MODIFICAR_SERVICIOS"));
    }

    // Tests para validación de permisos
    @Test
    @DisplayName("Debe validar permiso correctamente sin lanzar excepción")
    void debeValidarPermisoCorrectamente() {
        assertDoesNotThrow(() ->
            permisosService.validarPermiso(usuarioOwner, "GESTIONAR_USUARIOS")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no tiene permiso")
    void debeLanzarExcepcionCuandoNoTienePermiso() {
        assertThrows(PermisoInsuficienteException.class, () ->
            permisosService.validarPermiso(usuarioEmpleado, "CAMBIAR_PLAN")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando recepcionista intenta eliminar cliente")
    void debeLanzarExcepcionRecepcionistaEliminarCliente() {
        assertThrows(PermisoInsuficienteException.class, () ->
            permisosService.validarPermiso(usuarioRecepcionista, "ELIMINAR_CLIENTES")
        );
    }

    // Tests para gestión jerárquica de roles
    @Test
    @DisplayName("OWNER puede gestionar todos los roles")
    void ownerPuedeGestionarTodosLosRoles() {
        assertTrue(permisosService.puedeGestionarRol(UsuarioRol.OWNER, UsuarioRol.ADMIN));
        assertTrue(permisosService.puedeGestionarRol(UsuarioRol.OWNER, UsuarioRol.EMPLEADO));
        assertTrue(permisosService.puedeGestionarRol(UsuarioRol.OWNER, UsuarioRol.RECEPCIONISTA));
        assertTrue(permisosService.puedeGestionarRol(UsuarioRol.OWNER, UsuarioRol.OWNER));
    }

    @Test
    @DisplayName("ADMIN puede gestionar solo empleados y recepcionistas")
    void adminPuedeGestionarSoloEmpleadosRecepcionistas() {
        assertTrue(permisosService.puedeGestionarRol(UsuarioRol.ADMIN, UsuarioRol.EMPLEADO));
        assertTrue(permisosService.puedeGestionarRol(UsuarioRol.ADMIN, UsuarioRol.RECEPCIONISTA));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.ADMIN, UsuarioRol.ADMIN));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.ADMIN, UsuarioRol.OWNER));
    }

    @Test
    @DisplayName("EMPLEADO no puede gestionar ningún rol")
    void empleadoNoPuedeGestionarNingunRol() {
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.EMPLEADO, UsuarioRol.OWNER));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.EMPLEADO, UsuarioRol.ADMIN));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.EMPLEADO, UsuarioRol.EMPLEADO));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.EMPLEADO, UsuarioRol.RECEPCIONISTA));
    }

    @Test
    @DisplayName("RECEPCIONISTA no puede gestionar ningún rol")
    void recepcionistaNoPuedeGestionarNingunRol() {
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.RECEPCIONISTA, UsuarioRol.OWNER));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.RECEPCIONISTA, UsuarioRol.ADMIN));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.RECEPCIONISTA, UsuarioRol.EMPLEADO));
        assertFalse(permisosService.puedeGestionarRol(UsuarioRol.RECEPCIONISTA, UsuarioRol.RECEPCIONISTA));
    }

    @Test
    @DisplayName("Debe validar gestión de rol correctamente sin lanzar excepción")
    void debeValidarGestionRolCorrectamente() {
        assertDoesNotThrow(() ->
            permisosService.validarGestionRol(usuarioOwner, "admin")
        );

        assertDoesNotThrow(() ->
            permisosService.validarGestionRol(usuarioAdmin, "empleado")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando admin intenta gestionar owner")
    void debeLanzarExcepcionAdminGestionarOwner() {
        assertThrows(PermisoInsuficienteException.class, () ->
            permisosService.validarGestionRol(usuarioAdmin, "owner")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando empleado intenta gestionar cualquier rol")
    void debeLanzarExcepcionEmpleadoGestionarRol() {
        assertThrows(PermisoInsuficienteException.class, () ->
            permisosService.validarGestionRol(usuarioEmpleado, "recepcionista")
        );
    }

    // Tests para verificación de roles específicos
    @Test
    @DisplayName("Debe identificar correctamente si es owner")
    void debeIdentificarOwner() {
        assertTrue(permisosService.esOwner(usuarioOwner));
        assertFalse(permisosService.esOwner(usuarioAdmin));
        assertFalse(permisosService.esOwner(usuarioEmpleado));
        assertFalse(permisosService.esOwner(usuarioRecepcionista));
    }

    @Test
    @DisplayName("Debe identificar correctamente si es admin o owner")
    void debeIdentificarAdminOOwner() {
        assertTrue(permisosService.esAdminOOwner(usuarioOwner));
        assertTrue(permisosService.esAdminOOwner(usuarioAdmin));
        assertFalse(permisosService.esAdminOOwner(usuarioEmpleado));
        assertFalse(permisosService.esAdminOOwner(usuarioRecepcionista));
    }

    // Tests para casos edge
    @Test
    @DisplayName("Debe retornar false cuando usuario es null")
    void debeRetornarFalseCuandoUsuarioNull() {
        assertFalse(permisosService.tienePermiso(null, "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("Debe retornar false cuando rol es null")
    void debeRetornarFalseCuandoRolNull() {
        usuarioOwner.setRol(null);
        assertFalse(permisosService.tienePermiso(usuarioOwner, "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("Debe retornar false cuando rol es inválido")
    void debeRetornarFalseCuandoRolInvalido() {
        usuarioOwner.setRol("rol_invalido");
        assertFalse(permisosService.tienePermiso(usuarioOwner, "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("Debe retornar false para usuario con rol inválido en esOwner")
    void debeRetornarFalseEsOwnerRolInvalido() {
        usuarioOwner.setRol("rol_invalido");
        assertFalse(permisosService.esOwner(usuarioOwner));
    }

    @Test
    @DisplayName("Debe retornar false para usuario con rol inválido en esAdminOOwner")
    void debeRetornarFalseEsAdminOOwnerRolInvalido() {
        usuarioOwner.setRol("rol_invalido");
        assertFalse(permisosService.esAdminOOwner(usuarioOwner));
    }

    @Test
    @DisplayName("Debe obtener permisos correctos para cada rol")
    void debeObtenerPermisosCorrectosPorRol() {
        Set<String> permisosOwner = permisosService.obtenerPermisos(UsuarioRol.OWNER);
        Set<String> permisosAdmin = permisosService.obtenerPermisos(UsuarioRol.ADMIN);
        Set<String> permisosEmpleado = permisosService.obtenerPermisos(UsuarioRol.EMPLEADO);
        Set<String> permisosRecepcionista = permisosService.obtenerPermisos(UsuarioRol.RECEPCIONISTA);

        assertTrue(permisosOwner.size() > permisosAdmin.size());
        assertTrue(permisosAdmin.size() > permisosEmpleado.size());
        assertTrue(permisosEmpleado.size() > permisosRecepcionista.size());

        assertTrue(permisosOwner.contains("CAMBIAR_PLAN"));
        assertFalse(permisosAdmin.contains("CAMBIAR_PLAN"));

        assertTrue(permisosAdmin.contains("ELIMINAR_CLIENTES"));
        assertFalse(permisosEmpleado.contains("ELIMINAR_CLIENTES"));
    }

    @Test
    @DisplayName("Debe retornar set vacío para rol inexistente")
    void debeRetornarSetVacioParaRolInexistente() {
        // Intentar obtener permisos de un rol null
        Set<String> permisos = permisosService.obtenerPermisos(null);
        assertNotNull(permisos);
        assertTrue(permisos.isEmpty());
    }
}
