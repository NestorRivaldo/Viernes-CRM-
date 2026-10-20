package com.viernes.equipos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipoRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    String nombre,

    @Size(max = 500)
    String descripcion
) {}
