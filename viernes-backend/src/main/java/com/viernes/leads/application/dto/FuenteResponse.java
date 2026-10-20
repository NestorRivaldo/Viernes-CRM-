package com.viernes.leads.application.dto;

import java.time.LocalDateTime;

public record FuenteResponse(
    Long id,
    String codigo,
    String nombre,
    String descripcion,
    Boolean activa,
    LocalDateTime creadoEn
) {}
