package com.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_recordatorios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recordatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoRecordatorio tipo; // SMS, EMAIL, WHATSAPP

    @Column(nullable = false)
    @Builder.Default
    private boolean enviado = false;

    private LocalDateTime fechaEnvio;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TipoRecordatorio {
        SMS, EMAIL, WHATSAPP
    }
}