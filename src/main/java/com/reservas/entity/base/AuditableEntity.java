package com.reservas.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Clase base para entidades que requieren auditoría de timestamps.
 *
 * Proporciona automáticamente:
 * - createdAt: Fecha de creación (se establece una sola vez)
 * - updatedAt: Fecha de última modificación (se actualiza en cada cambio)
 *
 * Para usar esta clase:
 * 1. Extender de AuditableEntity
 * 2. Asegurar que @EnableJpaAuditing esté configurado en la aplicación
 *
 * @author Cita Click
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
