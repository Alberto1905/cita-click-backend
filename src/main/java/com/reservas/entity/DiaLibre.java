package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_dias_libres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaLibre {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false)
    private LocalDate fecha;

    private String razon;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}