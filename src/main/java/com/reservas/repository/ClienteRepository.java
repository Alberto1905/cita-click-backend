package com.reservas.repository;

import com.reservas.entity.Cliente;
import com.reservas.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByNegocio(Negocio negocio);

    Optional<Cliente> findByNegocioAndEmail(Negocio negocio, String email);

    boolean existsByNegocioAndEmail(Negocio negocio, String email);

    @Query("SELECT c FROM Cliente c WHERE c.negocio = :negocio AND " +
           "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Cliente> searchClientes(@Param("negocio") Negocio negocio, @Param("search") String search);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.negocio.id = :negocioId")
    long countByNegocioId(@Param("negocioId") UUID negocioId);
}
