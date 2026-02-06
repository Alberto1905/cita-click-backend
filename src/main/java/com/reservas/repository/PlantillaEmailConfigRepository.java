package com.reservas.repository;

import com.reservas.entity.Negocio;
import com.reservas.entity.PlantillaEmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para gestionar configuraciones de plantillas de email
 */
@Repository
public interface PlantillaEmailConfigRepository extends JpaRepository<PlantillaEmailConfig, String> {

    /**
     * Busca la configuración de plantilla por negocio
     * @param negocio Negocio a buscar
     * @return Optional con la configuración si existe
     */
    Optional<PlantillaEmailConfig> findByNegocio(Negocio negocio);

    /**
     * Verifica si existe una configuración para un negocio
     * @param negocio Negocio a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByNegocio(Negocio negocio);

    /**
     * Elimina la configuración de un negocio
     * @param negocio Negocio cuya configuración se eliminará
     */
    void deleteByNegocio(Negocio negocio);
}
