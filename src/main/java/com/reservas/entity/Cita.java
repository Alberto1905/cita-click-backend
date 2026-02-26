package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Data
@Entity
@Table(name = "tbl_citas")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoCita estado = EstadoCita.CONFIRMADA;

    private String notas;

    private BigDecimal precio;

    @Column(nullable = false)
    @Builder.Default
    private boolean recordatorioEnviado = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean pagado = false;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    // Campos para citas recurrentes
    @Column(name = "tipo_recurrencia")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TipoRecurrencia tipoRecurrencia = TipoRecurrencia.NO_RECURRENTE;

    @Column(name = "intervalo_recurrencia")
    private Integer intervaloRecurrencia; // Para recurrencia personalizada (ej: cada 3 días)

    @Column(name = "fecha_fin_recurrencia")
    private LocalDateTime fechaFinRecurrencia; // Fecha hasta la cual se repite

    @Column(name = "numero_ocurrencias")
    private Integer numeroOcurrencias; // O número máximo de repeticiones

    @Column(name = "dias_semana")
    private String diasSemana; // Para recurrencia semanal: "LUN,MIE,VIE"

    @Column(name = "cita_padre_id")
    private String citaPadreId; // ID de la cita original si esta es generada por recurrencia

    @Column(name = "es_recurrente")
    @Builder.Default
    private boolean esRecurrente = false;

    // Relación con servicios adicionales (además del servicio principal)
    @OneToMany(mappedBy = "cita", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CitaServicio> serviciosAdicionales = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum EstadoCita {
        PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA
    }
}