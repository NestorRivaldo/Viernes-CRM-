package com.viernes.equipos.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_EQUIPO_MIEMBROS", uniqueConstraints =
        @UniqueConstraint(name = "UK_EQUIPO_MIEMBRO", columnNames = {"EQUIPO_ID", "USUARIO_ID"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipoMiembroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EQUIPO_ID", nullable = false)
    private Long equipoId;

    @Column(name = "USUARIO_ID", nullable = false)
    private Long usuarioId;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
