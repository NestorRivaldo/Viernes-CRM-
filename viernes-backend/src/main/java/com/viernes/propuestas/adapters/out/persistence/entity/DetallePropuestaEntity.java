package com.viernes.propuestas.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "CRM_PROPUESTA_DETALLES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetallePropuestaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_PRODUCTO")
    private Long idProducto;

    @Column(name = "DESCRIPCION", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "SUBTOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;
}
