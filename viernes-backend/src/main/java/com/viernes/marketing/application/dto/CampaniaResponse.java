package com.viernes.marketing.application.dto;

import com.viernes.marketing.domain.EstadoCampania;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CampaniaResponse(
    Long id,
    String nombre,
    String descripcion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    EstadoCampania estado,
    LocalDateTime creadoEn
) {}
