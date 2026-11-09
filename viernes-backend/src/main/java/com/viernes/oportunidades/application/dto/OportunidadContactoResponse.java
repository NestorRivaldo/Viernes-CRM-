package com.viernes.oportunidades.application.dto;

import java.time.LocalDateTime;

public record OportunidadContactoResponse(
        Long id,
        Long idOportunidad,
        Long contactoId,
        String nombreContacto,
        String correoContacto,
        String rol,
        Boolean esDecisor,
        LocalDateTime creadoEn
) {}
