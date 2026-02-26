package com.reservas.service;

import com.reservas.dto.request.ActualizarPerfilRequest;
import com.reservas.dto.request.GoogleAuthRequest;
import com.reservas.dto.request.LoginRequest;
import com.reservas.dto.request.RegisterRequest;
import com.reservas.dto.response.GoogleUserInfo;
import com.reservas.dto.response.LoginResponse;
import com.reservas.dto.response.UserResponse;
import com.reservas.entity.HorarioTrabajo;
import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.HorarioTrabajoRepository;
import com.reservas.repository.NegocioRepository;
import com.reservas.repository.UsuarioRepository;
import com.reservas.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final HorarioTrabajoRepository horarioTrabajoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GoogleOAuthService googleOAuthService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UsuarioRepository usuarioRepository,
                       NegocioRepository negocioRepository,
                       HorarioTrabajoRepository horarioTrabajoRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       GoogleOAuthService googleOAuthService,
                       EmailVerificationService emailVerificationService) {
        this.usuarioRepository = usuarioRepository;
        this.negocioRepository = negocioRepository;
        this.horarioTrabajoRepository = horarioTrabajoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.googleOAuthService = googleOAuthService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Registra un nuevo usuario y su negocio
     */
    @Transactional
    public LoginResponse registrar(RegisterRequest request, HttpServletRequest httpRequest) {
        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new UnauthorizedException("El email ya está registrado");
        }

        // Crear negocio
        String nombreNegocio = (request.getNombreNegocio() != null && !request.getNombreNegocio().isBlank())
            ? request.getNombreNegocio()
            : request.getNombre() + "'s Business";

        String tipoNegocio = (request.getTipoNegocio() != null && !request.getTipoNegocio().isBlank())
            ? request.getTipoNegocio()
            : "salon";

        // Determinar el plan (por defecto basico si no se especifica)
        String plan = (request.getPlan() != null && !request.getPlan().isBlank())
            ? request.getPlan()
            : "basico";

        Negocio negocio = Negocio.builder()
                .email(request.getEmail())
                .nombre(nombreNegocio)
                .tipo(tipoNegocio)
                .plan(plan)
                .fechaInicioPlan(LocalDateTime.now())
                .build();
        // La entidad Negocio maneja el @PrePersist para configurar el trial automáticamente

        Negocio negocioGuardado = negocioRepository.save(negocio);

        // Crear usuario
        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .negocio(negocioGuardado)
                .rol("admin")
                .activo(true)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Enviar email de verificación
        emailVerificationService.enviarEmailVerificacion(usuarioGuardado);

        String token = jwtProvider.generateToken(usuarioGuardado.getEmail());

        log.info("Usuario y negocio registrados: {}", usuarioGuardado.getEmail());

        return LoginResponse.builder()
                .token(token)
                .nombre(usuarioGuardado.getNombre())
                .email(usuarioGuardado.getEmail())
                .negocioId(negocioGuardado.getId().toString())
                .build();
    }

    /**
     * Login de usuario
     */
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email o contraseña inválidos"));

        // Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new UnauthorizedException("Email o contraseña inválidos");
        }

        // Validar que está activo
        if (!usuario.isActivo()) {
            throw new UnauthorizedException("Usuario inactivo");
        }

        

        String token = jwtProvider.generateToken(usuario.getEmail());

        log.info("Login exitoso: {}", usuario.getEmail());

        return LoginResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                //.negocioId(negocioGuardado.getId().toString())
                .build();
    }

    /**
     * Obtener usuario por email
     */
    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    /**
     * Obtener información del usuario autenticado
     */
    @Transactional(readOnly = true)
    public UserResponse obtenerUsuarioActual() {
        // Obtener email del usuario autenticado del contexto de seguridad
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null || email.equals("anonymousUser")) {
            log.error("Intento de acceso sin autenticación");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        // Buscar usuario en la base de datos
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        // Validar que el usuario esté activo
        if (!usuario.isActivo()) {
            log.warn("Intento de acceso de usuario inactivo: {}", email);
            throw new UnauthorizedException("Usuario inactivo");
        }

        // Construir respuesta con información del usuario
        UserResponse response = UserResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidoPaterno(usuario.getApellidoPaterno())
                .apellidoMaterno(usuario.getApellidoMaterno())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .authProvider(usuario.getAuthProvider() != null ? usuario.getAuthProvider() : "local")
                .build();

        // Agregar información del negocio si está disponible
        if (usuario.getNegocio() != null) {
            response.setNegocioId(usuario.getNegocio().getId());
            response.setNombreNegocio(usuario.getNegocio().getNombre());
        }

        return response;
    }

    /**
     * Autenticación con Google OAuth2
     * Si el usuario ya existe, hace login
     * Si no existe, lo registra automáticamente
     */
    @Transactional
    public LoginResponse googleAuth(GoogleAuthRequest request, HttpServletRequest httpRequest) {
        log.debug("Iniciando autenticación con Google");

        // 1. Verificar token de Google
        GoogleUserInfo googleUser = googleOAuthService.verifyGoogleToken(request.getIdToken());

        // 2. Buscar usuario por email
        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(googleUser.getEmail());

        Usuario usuario;
        Negocio negocio;

        if (usuarioExistente.isPresent()) {
            // Usuario ya existe - LOGIN
            usuario = usuarioExistente.get();
            negocio = usuario.getNegocio();

            // Validar que está activo
            if (!usuario.isActivo()) {
                throw new UnauthorizedException("Usuario inactivo");
            }

            // Actualizar información de Google si cambió
            if (usuario.getAuthProvider() == null || !usuario.getAuthProvider().equals("google")) {
                usuario.setAuthProvider("google");
                usuario.setProviderId(googleUser.getGoogleId());
            }
            if (googleUser.getImageUrl() != null && !googleUser.getImageUrl().equals(usuario.getImageUrl())) {
                usuario.setImageUrl(googleUser.getImageUrl());
            }

            usuarioRepository.save(usuario);
            log.info("Login con Google exitoso para usuario existente: {}", googleUser.getEmail());

        } else {
            // Usuario no existe - REGISTRO
            log.info("Registrando nuevo usuario desde Google: {}", googleUser.getEmail());

            // Crear negocio
            String nombreNegocio = (request.getNombreNegocio() != null && !request.getNombreNegocio().isBlank())
                    ? request.getNombreNegocio()
                    : googleUser.getNombreCompleto() + "'s Business";

            String tipoNegocio = (request.getTipoNegocio() != null && !request.getTipoNegocio().isBlank())
                    ? request.getTipoNegocio()
                    : "salon";

            // Determinar el plan (por defecto basico)
            String plan = (request.getPlan() != null && !request.getPlan().isBlank())
                    ? request.getPlan()
                    : "basico";

            negocio = Negocio.builder()
                    .email(googleUser.getEmail())
                    .nombre(nombreNegocio)
                    .tipo(tipoNegocio)
                    .plan(plan)
                    .fechaInicioPlan(LocalDateTime.now())
                    .build();
            // La entidad Negocio maneja el @PrePersist para configurar el trial automáticamente

            negocio = negocioRepository.save(negocio);

            // Crear usuario
            // Para usuarios de Google, separamos el nombre completo si no hay apellido
            String nombre = googleUser.getNombre();
            String apellidoPaterno = googleUser.getApellido();
            String apellidoMaterno = "";

            // Si no hay nombre o apellido, usar el nombreCompleto
            if (nombre.isBlank() && apellidoPaterno.isBlank()) {
                String[] partes = googleUser.getNombreCompleto().split(" ", 3);
                nombre = partes.length > 0 ? partes[0] : googleUser.getEmail();
                apellidoPaterno = partes.length > 1 ? partes[1] : "Usuario";
                apellidoMaterno = partes.length > 2 ? partes[2] : "";
            }

            usuario = Usuario.builder()
                    .email(googleUser.getEmail())
                    .passwordHash(null) // Sin password para usuarios OAuth
                    .nombre(nombre.isBlank() ? googleUser.getEmail() : nombre)
                    .apellidoPaterno(apellidoPaterno.isBlank() ? "Usuario" : apellidoPaterno)
                    .apellidoMaterno(apellidoMaterno)
                    .authProvider("google")
                    .providerId(googleUser.getGoogleId())
                    .imageUrl(googleUser.getImageUrl())
                    .negocio(negocio)
                    .rol("admin")
                    .activo(true)
                    .build();

            usuario = usuarioRepository.save(usuario);

            // Enviar email de verificación (igual que en registro normal)
            emailVerificationService.enviarEmailVerificacion(usuario);

            log.info("Usuario registrado exitosamente desde Google: {}", googleUser.getEmail());
        }

        // Generar JWT token
        String token = jwtProvider.generateToken(usuario.getEmail());

        return LoginResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .negocioId(negocio.getId().toString())
                .build();
    }

    /**
     * Actualizar perfil del usuario autenticado.
     * Permite cambiar nombre, apellidos, teléfono y contraseña (opcional).
     * Los usuarios de Google no pueden cambiar la contraseña.
     */
    @Transactional
    public UserResponse actualizarPerfil(ActualizarPerfilRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null || email.equals("anonymousUser")) {
            throw new UnauthorizedException("Usuario no autenticado");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        // Actualizar campos básicos
        usuario.setNombre(request.getNombre());
        usuario.setApellidoPaterno(request.getApellidoPaterno());
        usuario.setApellidoMaterno(request.getApellidoMaterno());
        usuario.setTelefono(request.getTelefono());

        // Cambio de contraseña (solo para usuarios locales)
        if (request.getPasswordNueva() != null && !request.getPasswordNueva().isBlank()) {
            if (usuario.getAuthProvider() != null && !usuario.getAuthProvider().equals("local")) {
                throw new IllegalArgumentException("Los usuarios de Google no pueden cambiar la contraseña desde aquí");
            }
            if (request.getPasswordActual() == null || request.getPasswordActual().isBlank()) {
                throw new IllegalArgumentException("La contraseña actual es requerida para cambiarla");
            }
            if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            }
            usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Perfil actualizado para usuario: {}", email);

        UserResponse response = UserResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidoPaterno(usuario.getApellidoPaterno())
                .apellidoMaterno(usuario.getApellidoMaterno())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .authProvider(usuario.getAuthProvider() != null ? usuario.getAuthProvider() : "local")
                .build();

        if (usuario.getNegocio() != null) {
            response.setNegocioId(usuario.getNegocio().getId());
            response.setNombreNegocio(usuario.getNegocio().getNombre());
        }

        return response;
    }

}