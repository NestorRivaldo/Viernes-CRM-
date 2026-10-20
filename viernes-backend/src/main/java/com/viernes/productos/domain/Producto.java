package com.viernes.productos.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String categoria;
    private BigDecimal precioBase;
    private EstadoProducto estado;
    private LocalDateTime creadoEn;
}
