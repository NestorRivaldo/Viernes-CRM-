package com.viernes.oportunidades.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CRM_OPORTUNIDAD_PRODUCTOS", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oportunidad_producto", columnNames = {"ID_OPORTUNIDAD", "ID_PRODUCTO"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OportunidadProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_OPORTUNIDAD", nullable = false)
    private Long idOportunidad;

    // Referencia lógica a CRM_PRODUCTOS (módulo productos), sin foreign key física
    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long idProducto;

    @Column(name = "CANTIDAD")
    private Integer cantidad;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
