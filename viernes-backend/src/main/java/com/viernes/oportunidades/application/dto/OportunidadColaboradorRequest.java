package com.viernes.oportunidades.application.dto;

import jakarta.validation.constraints.NotNull;

public record OportunidadColaboradorRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long usuarioId
) {}
