package com.viernes.equipos.application.dto;

import java.time.LocalDateTime;

public record EquipoResponse(
    Long id,
    String nombre,
    String descripcion,
    Boolean activo,
    LocalDateTime creadoEn,
    long totalMiembros
) {}
