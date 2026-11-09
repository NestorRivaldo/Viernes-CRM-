package com.viernes.oportunidades.application.dto;

import java.time.LocalDateTime;

public record OportunidadColaboradorResponse(
        Long id,
        Long idOportunidad,
        Long usuarioId,
        String nombreCompleto,
        String correo,
        LocalDateTime creadoEn
) {}
