package com.viernes.oportunidades.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_OPORTUNIDAD_CONTACTOS", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oportunidad_contacto", columnNames = {"ID_OPORTUNIDAD", "ID_CONTACTO"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadContactoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_OPORTUNIDAD", nullable = false)
    private Long idOportunidad;

    // Referencia lógica a CRM_CONTACTOS (módulo clientes), sin foreign key física
    @Column(name = "ID_CONTACTO", nullable = false)
    private Long idContacto;

    @Column(name = "ROL", length = 100)
    private String rol;

    @Column(name = "ES_DECISOR")
    @Builder.Default
    private Boolean esDecisor = false;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
