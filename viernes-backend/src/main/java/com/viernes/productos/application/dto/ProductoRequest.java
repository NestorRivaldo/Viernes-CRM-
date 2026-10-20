package com.viernes.productos.application.dto;

import com.viernes.productos.domain.EstadoProducto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 200) String nombre,
    @Size(max = 1000) String descripcion,
    @Size(max = 100) String categoria,
    @NotNull @PositiveOrZero BigDecimal precioBase,
    EstadoProducto estado
) {}
