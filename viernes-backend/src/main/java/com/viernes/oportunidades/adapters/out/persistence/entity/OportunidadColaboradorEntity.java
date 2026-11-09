package com.viernes.oportunidades.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_OPORTUNIDAD_COLABORADORES", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oportunidad_colaborador", columnNames = {"ID_OPORTUNIDAD", "ID_USUARIO"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadColaboradorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_OPORTUNIDAD", nullable = false)
    private Long idOportunidad;

    // Referencia lógica a CRM_USUARIOS (módulo usuarios), sin foreign key física
    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
