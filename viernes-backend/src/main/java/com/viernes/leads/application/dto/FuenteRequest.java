package com.viernes.leads.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FuenteRequest(
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 120) String nombre,
    @Size(max = 500) String descripcion,
    Boolean activa
) {}
