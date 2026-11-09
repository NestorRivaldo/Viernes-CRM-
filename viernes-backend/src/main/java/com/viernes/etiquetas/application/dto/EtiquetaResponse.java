package com.viernes.etiquetas.application.dto;

import java.time.LocalDateTime;

public record EtiquetaResponse(
        Long id,
        String nombre,
        String color,
        String descripcion,
        Boolean activa,
        LocalDateTime creadoEn
) {
}
