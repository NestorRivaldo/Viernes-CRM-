package com.viernes.oportunidades.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OportunidadContactoRequest(
        @NotNull(message = "El contacto es obligatorio")
        Long contactoId,
        @Size(max = 100) String rol,
        Boolean esDecisor
) {}
