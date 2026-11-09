package com.viernes.oportunidades.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OportunidadProductoRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad
) {}
