package com.viernes.oportunidades.application.dto;

import com.viernes.oportunidades.domain.EtapaOportunidad;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OportunidadRequest(
    @NotBlank @Size(max = 200) String nombre,
    @Size(max = 150) String clienteNombre,
    BigDecimal valor,
    @Min(0) @Max(100) Integer probabilidad,
    EtapaOportunidad etapa,
    @Size(max = 100) String responsable,
    LocalDate fechaCierre
) {}
