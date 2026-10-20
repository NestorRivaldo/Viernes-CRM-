package com.viernes.actividades.application.dto;

import jakarta.validation.constraints.NotNull;

public record ParticipanteRequest(
        @NotNull(message = "El contacto es obligatorio")
        Long contactoId
) {}
