package com.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;
@Data
@Entity
@Table(name = "tbl_horarios_trabajo", schema = "ccdiad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false)
    private Integer diaSemana; // 0-6 (Lunes-Domingo)

    @Column(nullable = false)
    private LocalTime horaApertura;

    @Column(nullable = false)
    private LocalTime horaCierre;

    @Column(columnDefinition = "boolean default true")
    @Builder.Default
    private boolean activo = true;
}