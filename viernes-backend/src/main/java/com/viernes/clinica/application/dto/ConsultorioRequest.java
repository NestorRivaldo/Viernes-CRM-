package com.viernes.clinica.application.dto;

import com.viernes.clinica.domain.EstadoConsultorio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ConsultorioRequest(
        @NotNull(message = "La clínica es obligatoria")
        Long idClinica,

        @NotBlank(message = "El código es obligatorio")
        String codigo,

        @NotNull(message = "La tarifa es obligatoria")
        @DecimalMin(value = "0.01", message = "La tarifa debe ser mayor a 0")
        BigDecimal tarifaHora,

        @NotNull(message = "El estado es obligatorio")
        EstadoConsultorio estado,

        List<Long> especialidadIds
) {
}
