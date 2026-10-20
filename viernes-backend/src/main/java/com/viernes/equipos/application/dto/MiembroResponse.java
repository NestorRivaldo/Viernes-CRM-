package com.viernes.equipos.application.dto;

import java.time.LocalDateTime;

public record MiembroResponse(
    Long usuarioId,
    String nombreCompleto,
    String correo,
    LocalDateTime agregadoEn
) {}
