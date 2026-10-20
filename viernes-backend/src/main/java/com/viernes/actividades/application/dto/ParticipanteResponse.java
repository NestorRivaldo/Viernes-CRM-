package com.viernes.actividades.application.dto;

import java.time.LocalDateTime;

public record ParticipanteResponse(
        Long id,
        Long idActividad,
        Long contactoId,
        String nombreContacto,
        String correoContacto,
        LocalDateTime creadoEn
) {}
