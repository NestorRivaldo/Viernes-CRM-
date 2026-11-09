package com.viernes.etiquetas.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_ETIQUETAS", uniqueConstraints = {
        @UniqueConstraint(name = "uk_etiqueta_nombre_normalizado", columnNames = "NOMBRE_NORMALIZADO")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtiquetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ETIQUETA")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 40)
    private String nombre;

    @Column(name = "NOMBRE_NORMALIZADO", nullable = false, length = 80)
    private String nombreNormalizado;

    @Column(name = "COLOR", nullable = false, length = 7)
    private String color;

    @Column(name = "DESCRIPCION", length = 160)
    private String descripcion;

    @Column(name = "ACTIVA", nullable = false)
    private Boolean activa;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
