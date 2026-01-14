package com.reservas.repository;

import com.reservas.entity.Negocio;
import com.reservas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    /**
     * Busca un usuario por email con el negocio cargado eagerly usando JOIN FETCH.
     * Esto previene LazyInitializationException al acceder a propiedades del negocio.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.negocio WHERE u.email = :email")
    Optional<Usuario> findByEmailWithNegocio(@Param("email") String email);

    boolean existsByEmail(String email);

    List<Usuario> findByNegocio(Negocio negocio);

    List<Usuario> findByNegocioAndActivo(Negocio negocio, boolean activo);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.negocio.id = :negocioId AND u.activo = true")
    long countActiveUsuariosByNegocioId(@Param("negocioId") UUID negocioId);

    @Query("SELECT u FROM Usuario u WHERE u.negocio.id = :negocioId AND u.rol = :rol AND u.activo = true")
    List<Usuario> findByNegocioIdAndRol(@Param("negocioId") UUID negocioId, @Param("rol") String rol);

    Optional<Usuario> findByNegocioAndEmailAndActivo(Negocio negocio, String email, boolean activo);

    Optional<Usuario> findByTokenVerificacion(String token);
}
