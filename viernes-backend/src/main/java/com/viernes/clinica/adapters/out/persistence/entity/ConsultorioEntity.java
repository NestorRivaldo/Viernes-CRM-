package com.viernes.clinica.adapters.out.persistence.entity;

import com.viernes.clinica.domain.EstadoConsultorio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CONSULTORIOS", schema = "EXA_CLINICA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultorioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_CLINICA", nullable = false)
    private ClinicaEntity clinica;

    @Column(name = "CODIGO", length = 10, nullable = false, unique = true)
    private String codigo;

    @Column(name = "TARIFA_HORA", precision = 8, scale = 2, nullable = false)
    private BigDecimal tarifaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20, nullable = false)
    private EstadoConsultorio estado;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "CONSULTORIO_ESPECIALIDAD",
        schema = "EXA_CLINICA",
        joinColumns = @JoinColumn(name = "ID_CONSULTORIO"),
        inverseJoinColumns = @JoinColumn(name = "ID_ESPECIALIDAD")
    )
    @Builder.Default
    private Set<EspecialidadEntity> especialidades = new HashSet<>();
}
