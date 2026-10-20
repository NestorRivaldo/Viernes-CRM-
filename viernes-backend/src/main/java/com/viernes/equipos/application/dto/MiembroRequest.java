package com.viernes.equipos.application.dto;

import jakarta.validation.constraints.NotNull;

public record MiembroRequest(
    @NotNull(message = "El usuario es obligatorio")
    Long usuarioId
) {}
