package com.viernes.propuestas.application.dto;

import java.math.BigDecimal;

public record DetallePropuestaResponse(
    Long id,
    Long productoId,
    String descripcion,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {}
