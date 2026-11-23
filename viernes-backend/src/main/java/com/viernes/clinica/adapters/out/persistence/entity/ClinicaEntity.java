package com.viernes.clinica.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CLINICAS", schema = "EXA_CLINICA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUC", length = 11, nullable = false, unique = true)
    private String ruc;

    @Column(name = "RAZON_SOCIAL", length = 150, nullable = false)
    private String razonSocial;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Integer activo = 1;
}
