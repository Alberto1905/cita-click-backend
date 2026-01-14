package com.reservas.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.reservas.dto.response.GoogleUserInfo;
import com.reservas.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class GoogleOAuthService {

    @Value("${google.client.id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthService(@Value("${google.client.id}") String googleClientId) {
        this.googleClientId = googleClientId;

        // Inicializar el verificador de tokens de Google
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * Verifica y decodifica el ID Token de Google
     */
    public GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            log.info("Verificando token de Google...");

            // Verificar el token
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.error("Token de Google inválido o expirado");
                throw new UnauthorizedException("Token de Google inválido o expirado");
            }

            // Extraer información del payload
            Payload payload = idToken.getPayload();

            // Verificar que el email esté verificado
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.error("Email de Google no verificado");
                throw new UnauthorizedException("Email de Google no verificado");
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String nombre = (String) payload.get("given_name");
            String apellido = (String) payload.get("family_name");
            String nombreCompleto = (String) payload.get("name");
            String imageUrl = (String) payload.get("picture");

            log.info("Token de Google verificado exitosamente para: {}", email);

            return GoogleUserInfo.builder()
                    .googleId(googleId)
                    .email(email)
                    .nombre(nombre != null ? nombre : "")
                    .apellido(apellido != null ? apellido : "")
                    .nombreCompleto(nombreCompleto != null ? nombreCompleto : email)
                    .imageUrl(imageUrl)
                    .emailVerified(true)
                    .build();

        } catch (Exception e) {
            log.error("Error al verificar token de Google: {}", e.getMessage(), e);
            throw new UnauthorizedException("Error al verificar token de Google: " + e.getMessage());
        }
    }
}
