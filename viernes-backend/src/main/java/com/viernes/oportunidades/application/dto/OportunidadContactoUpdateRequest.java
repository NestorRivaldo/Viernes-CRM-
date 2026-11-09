package com.viernes.oportunidades.application.dto;

import jakarta.validation.constraints.Size;

public record OportunidadContactoUpdateRequest(
        @Size(max = 100) String rol,
        Boolean esDecisor
) {}
