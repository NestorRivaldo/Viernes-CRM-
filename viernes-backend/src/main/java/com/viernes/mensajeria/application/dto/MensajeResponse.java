package com.viernes.mensajeria.application.dto;

import java.time.LocalDateTime;

public record MensajeResponse(
        Long id,
        String usuarioCorreo,
        String usuarioNombre,
        String contenido,
        LocalDateTime creadoEn
) {}
