package com.viernes.productos.application.dto;

import com.viernes.productos.domain.EstadoProducto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
    Long id,
    String codigo,
    String nombre,
    String descripcion,
    String categoria,
    BigDecimal precioBase,
    EstadoProducto estado,
    LocalDateTime creadoEn
) {}
