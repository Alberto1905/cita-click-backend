package com.reservas.entity;

import com.reservas.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad que representa un cliente de un negocio.
 *
 * Un cliente pertenece a un negocio específico y puede tener múltiples citas.
 *
 * @author Cita Click
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "tbl_clientes",
    indexes = {
        @Index(name = "idx_cliente_negocio_id", columnList = "negocio_id"),
        @Index(name = "idx_cliente_email", columnList = "email"),
        @Index(name = "idx_cliente_telefono", columnList = "telefono"),
        @Index(name = "idx_cliente_negocio_email", columnList = "negocio_id, email"),
        @Index(name = "idx_cliente_negocio_telefono", columnList = "negocio_id, telefono")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cliente_negocio"))
    private Negocio negocio;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido_paterno", length = 100)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "genero", length = 30)
    private String genero; // Masculino, Femenino, Otro, Prefiero no decir

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    /**
     * Método helper para obtener el nombre completo del cliente.
     *
     * @return Nombre completo concatenado
     */
    public String getNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder(nombre);
        if (apellidoPaterno != null && !apellidoPaterno.isEmpty()) {
            nombreCompleto.append(" ").append(apellidoPaterno);
        }
        if (apellidoMaterno != null && !apellidoMaterno.isEmpty()) {
            nombreCompleto.append(" ").append(apellidoMaterno);
        }
        return nombreCompleto.toString();
    }

    /**
     * Verifica si el cliente tiene información de contacto completa.
     *
     * @return true si tiene email o teléfono
     */
    public boolean tieneContacto() {
        return (email != null && !email.isEmpty()) || (telefono != null && !telefono.isEmpty());
    }
}
