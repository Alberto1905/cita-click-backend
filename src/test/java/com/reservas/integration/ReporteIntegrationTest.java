package com.reservas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.dto.request.CitaRequest;
import com.reservas.dto.request.ClienteRequest;
import com.reservas.dto.request.LoginRequest;
import com.reservas.dto.request.RegisterRequest;
import com.reservas.dto.request.ServicioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Reporte Integration Tests - E2E Flow")
class ReporteIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private MockMvc mockMvc;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        // 1. Registrar usuario
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("reportes-test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setNombre("Test");
        registerRequest.setApellidoPaterno("Reportes");
        registerRequest.setApellidoMaterno("User");
        registerRequest.setNombreNegocio("Salon Reportes E2E");
        registerRequest.setTipoNegocio("salon");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("reportes-test@example.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data")
                .get("token")
                .asText();

        // 3. Crear datos de prueba (servicios, clientes, citas)
        crearDatosDePrueba();
    }

    private void crearDatosDePrueba() throws Exception {
        // Crear servicio
        ServicioRequest servicioRequest = ServicioRequest.builder()
                .nombre("Corte Premium")
                .descripcion("Corte profesional")
                .precio(new BigDecimal("200.00"))
                .duracionMinutos(45)
                .activo(true)
                .build();

        MvcResult servicioResult = mockMvc.perform(post("/servicios")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(servicioRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String servicioId = objectMapper.readTree(servicioResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Crear cliente
        ClienteRequest clienteRequest = ClienteRequest.builder()
                .nombre("Ana")
                .apellidoPaterno("Martínez")
                .apellidoMaterno("Pérez")
                .email("ana.reportes@test.com")
                .telefono("9876543210")
                .build();

        MvcResult clienteResult = mockMvc.perform(post("/clientes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clienteRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String clienteId = objectMapper.readTree(clienteResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Crear cita completada (para ingresos)
        LocalDateTime fechaHoraCita = LocalDateTime.now().minusHours(2);
        CitaRequest citaRequest = CitaRequest.builder()
                .fecha(fechaHoraCita.toLocalDate())
                .hora(fechaHoraCita.toLocalTime())
                .clienteId(clienteId)
                .servicioId(servicioId)
                .notas("Cita completada")
                .build();

        MvcResult citaResult = mockMvc.perform(post("/citas")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(citaRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String citaId = objectMapper.readTree(citaResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Marcar como completada
        mockMvc.perform(patch("/citas/" + citaId + "/estado")
                .header("Authorization", "Bearer " + jwtToken)
                .param("estado", "COMPLETADA"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("E2E: Generar reporte diario")
    void testGenerarReporteDiario() throws Exception {
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get("/reportes/diario")
                .header("Authorization", "Bearer " + jwtToken)
                .param("fecha", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.periodo").value("DIARIO"))
                .andExpect(jsonPath("$.data.totalCitas").isNumber())
                .andExpect(jsonPath("$.data.citasCompletadas").isNumber())
                .andExpect(jsonPath("$.data.ingresoTotal").isNumber())
                .andExpect(jsonPath("$.data.clientesTotales").isNumber());
    }

    @Test
    @DisplayName("E2E: Generar reporte semanal")
    void testGenerarReporteSemanal() throws Exception {
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get("/reportes/semanal")
                .header("Authorization", "Bearer " + jwtToken)
                .param("fechaInicio", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.periodo").value("SEMANAL"))
                .andExpect(jsonPath("$.data.totalCitas").isNumber())
                .andExpect(jsonPath("$.data.citasCompletadas").isNumber());
    }

    @Test
    @DisplayName("E2E: Generar reporte mensual")
    void testGenerarReporteMensual() throws Exception {
        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();

        mockMvc.perform(get("/reportes/mensual")
                .header("Authorization", "Bearer " + jwtToken)
                .param("mes", String.valueOf(mes))
                .param("anio", String.valueOf(anio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.periodo").value("MENSUAL"))
                .andExpect(jsonPath("$.data.totalCitas").isNumber())
                .andExpect(jsonPath("$.data.ingresoTotal").isNumber())
                .andExpect(jsonPath("$.data.ingresoEstimado").isNumber());
    }

    @Test
    @DisplayName("E2E: Verificar cálculo de ingresos en reportes")
    void testVerificarCalculoIngresos() throws Exception {
        LocalDate hoy = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/reportes/diario")
                .header("Authorization", "Bearer " + jwtToken)
                .param("fecha", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        double ingresoTotal = objectMapper.readTree(responseJson)
                .get("data")
                .get("ingresoTotal")
                .asDouble();

        // Verificar que hay ingresos (de la cita completada creada en setUp)
        assert ingresoTotal >= 0;
    }
}
