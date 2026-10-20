package com.viernes.leads.adapters.out.persistence.entity;

import com.viernes.leads.domain.EstadoLead;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_LEADS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "EMPRESA", length = 150)
    private String empresa;

    @Column(name = "CORREO", length = 150)
    private String correo;

    @Column(name = "TELEFONO", length = 30)
    private String telefono;

    @Column(name = "FUENTE", length = 80)
    private String fuente;

    @Column(name = "FUENTE_ID")
    private Long fuenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    @Builder.Default
    private EstadoLead estado = EstadoLead.NUEVO;

    @Column(name = "RESPONSABLE", length = 100)
    private String responsable;

    @Column(name = "FECHA")
    private LocalDate fecha;

    @Column(name = "CREADO_EN", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
