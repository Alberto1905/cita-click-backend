package com.reservas.service;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService - Pruebas Unitarias")
class RateLimitServiceTest {

    @InjectMocks
    private RateLimitService rateLimitService;

    private static final String TEST_KEY = "192.168.1.1:/api/test";

    @BeforeEach
    void setUp() {
        // Crear nueva instancia para cada test
        rateLimitService = new RateLimitService();
    }

    @Test
    @DisplayName("Debe permitir primera petición")
    void debePermitirPrimeraPeticion() {
        // Act
        boolean resultado = rateLimitService.tryConsume(TEST_KEY);

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Debe permitir hasta 5 peticiones")
    void debePermitirHasta5Peticiones() {
        // Act & Assert
        for (int i = 0; i < 5; i++) {
            boolean resultado = rateLimitService.tryConsume(TEST_KEY);
            assertTrue(resultado, "La petición " + (i + 1) + " debería estar permitida");
        }
    }

    @Test
    @DisplayName("Debe bloquear sexta petición")
    void debeBloquearSextaPeticion() {
        // Arrange - Consumir los 5 tokens disponibles
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsume(TEST_KEY);
        }

        // Act - Intentar sexta petición
        boolean resultado = rateLimitService.tryConsume(TEST_KEY);

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Debe crear bucket independiente para cada key")
    void debeCrearBucketIndependiente_paraCadaKey() {
        // Arrange
        String key1 = "192.168.1.1:/api/endpoint1";
        String key2 = "192.168.1.2:/api/endpoint2";

        // Act - Consumir 5 tokens del primer key
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsume(key1);
        }

        // Assert - El segundo key debería tener todos sus tokens
        boolean resultado = rateLimitService.tryConsume(key2);
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Debe retornar bucket existente para misma key")
    void debeRetornarBucketExistente_paraMismaKey() {
        // Act
        Bucket bucket1 = rateLimitService.resolveBucket(TEST_KEY);
        Bucket bucket2 = rateLimitService.resolveBucket(TEST_KEY);

        // Assert
        assertSame(bucket1, bucket2);
    }

    @Test
    @DisplayName("Debe obtener tokens disponibles correctamente")
    void debeObtenerTokensDisponibles_correctamente() {
        // Act - Estado inicial
        long tokensInicial = rateLimitService.getAvailableTokens(TEST_KEY);

        // Assert
        assertEquals(5, tokensInicial);
    }

    @Test
    @DisplayName("Debe decrementar tokens disponibles al consumir")
    void debeDecrementarTokensDisponibles_alConsumir() {
        // Act
        rateLimitService.tryConsume(TEST_KEY);
        long tokensRestantes = rateLimitService.getAvailableTokens(TEST_KEY);

        // Assert
        assertEquals(4, tokensRestantes);
    }

    @Test
    @DisplayName("Debe retornar cero tokens cuando se excede el límite")
    void debeRetornarCeroTokens_cuandoExcedeLimite() {
        // Arrange - Consumir todos los tokens
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsume(TEST_KEY);
        }

        // Act
        long tokensRestantes = rateLimitService.getAvailableTokens(TEST_KEY);

        // Assert
        assertEquals(0, tokensRestantes);
    }

    @Test
    @DisplayName("Debe manejar múltiples keys simultáneamente")
    void debeManejarMultiplesKeys_simultaneamente() {
        // Arrange
        String key1 = "192.168.1.1:/api/login";
        String key2 = "192.168.1.2:/api/login";
        String key3 = "192.168.1.3:/api/login";

        // Act - Consumir diferentes cantidades en cada key
        rateLimitService.tryConsume(key1);
        rateLimitService.tryConsume(key1);

        rateLimitService.tryConsume(key2);
        rateLimitService.tryConsume(key2);
        rateLimitService.tryConsume(key2);

        rateLimitService.tryConsume(key3);

        // Assert
        assertEquals(3, rateLimitService.getAvailableTokens(key1));
        assertEquals(2, rateLimitService.getAvailableTokens(key2));
        assertEquals(4, rateLimitService.getAvailableTokens(key3));
    }

    @Test
    @DisplayName("Debe manejar diferentes endpoints para misma IP")
    void debeManejarDiferentesEndpoints_paraMismaIP() {
        // Arrange
        String ip = "192.168.1.1";
        String keyEndpoint1 = ip + ":/api/login";
        String keyEndpoint2 = ip + ":/api/register";

        // Act - Agotar límite en endpoint1
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsume(keyEndpoint1);
        }

        // Assert - endpoint2 debería tener sus propios tokens
        boolean resultado = rateLimitService.tryConsume(keyEndpoint2);
        assertTrue(resultado);
        assertEquals(0, rateLimitService.getAvailableTokens(keyEndpoint1));
        assertEquals(4, rateLimitService.getAvailableTokens(keyEndpoint2));
    }

    @Test
    @DisplayName("Debe crear nuevo bucket cuando key no existe")
    void debeCrearNuevoBucket_cuandoKeyNoExiste() {
        // Act
        Bucket bucket = rateLimitService.resolveBucket("nueva-key-nunca-usada");

        // Assert
        assertNotNull(bucket);
        assertEquals(5, bucket.getAvailableTokens());
    }
}
