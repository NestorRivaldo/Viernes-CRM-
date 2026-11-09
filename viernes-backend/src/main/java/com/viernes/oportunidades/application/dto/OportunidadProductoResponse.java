package com.viernes.oportunidades.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OportunidadProductoResponse(
        Long id,
        Long idOportunidad,
        Long productoId,
        String codigo,
        String nombreProducto,
        BigDecimal precioBase,
        Integer cantidad,
        LocalDateTime creadoEn
) {}
