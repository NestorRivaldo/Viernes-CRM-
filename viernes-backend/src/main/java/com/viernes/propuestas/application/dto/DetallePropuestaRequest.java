package com.viernes.propuestas.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DetallePropuestaRequest(
    @Size(max = 500)
    String descripcion,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    Integer cantidad,

    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    BigDecimal precioUnitario,

    Long productoId
) {}
