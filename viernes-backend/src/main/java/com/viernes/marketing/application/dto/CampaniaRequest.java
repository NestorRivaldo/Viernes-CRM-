package com.viernes.marketing.application.dto;

import com.viernes.marketing.domain.EstadoCampania;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CampaniaRequest(
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 1000) String descripcion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    EstadoCampania estado
) {}
