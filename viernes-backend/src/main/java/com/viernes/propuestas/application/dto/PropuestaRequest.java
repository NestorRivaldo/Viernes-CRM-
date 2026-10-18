package com.viernes.propuestas.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropuestaRequest(
    @NotNull(message = "La oportunidad es obligatoria")
    Long idOportunidad,

    @NotNull(message = "El cliente es obligatorio")
    Long idCliente,

    @Size(max = 150)
    String clienteNombre,

    @Size(max = 100)
    String responsable,

    @NotNull(message = "La fecha de emisión es obligatoria")
    LocalDate fechaEmision,

    LocalDate fechaVencimiento,

    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    BigDecimal descuento,

    String observaciones,

    @NotEmpty(message = "La propuesta debe tener al menos un detalle")
    @Valid
    List<DetallePropuestaRequest> detalles
) {}
