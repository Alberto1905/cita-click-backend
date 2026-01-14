package com.reservas.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Obtener bucket para rate limiting
     * Configuración: 5 intentos cada 1 minuto
     */
    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Crear nuevo bucket con límite de 5 peticiones por minuto
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Verificar si se puede procesar la petición
     * @param key Identificador único (IP + endpoint)
     * @return true si se permite, false si excede el límite
     */
    public boolean tryConsume(String key) {
        Bucket bucket = resolveBucket(key);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn(" Rate limit excedido para: {}", key);
        }

        return consumed;
    }

    /**
     * Obtener tokens disponibles
     */
    public long getAvailableTokens(String key) {
        return resolveBucket(key).getAvailableTokens();
    }
}
