package com.viernes.clinica.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ESPECIALIDADES", schema = "EXA_CLINICA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecialidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", length = 10, nullable = false, unique = true)
    private String codigo;

    @Column(name = "NOMBRE", length = 100, nullable = false)
    private String nombre;
}
