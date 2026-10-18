package com.viernes.propuestas.application.dto;

import com.viernes.propuestas.domain.EstadoPropuesta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PropuestaResponse(
    Long id,
    String numeroPropuesta,
    Long idOportunidad,
    Long idCliente,
    String clienteNombre,
    EstadoPropuesta estado,
    Integer version,
    BigDecimal subtotal,
    BigDecimal descuento,
    BigDecimal total,
    String responsable,
    LocalDate fechaEmision,
    LocalDate fechaVencimiento,
    String observaciones,
    LocalDateTime creadoEn,
    List<DetallePropuestaResponse> detalles
) {}
