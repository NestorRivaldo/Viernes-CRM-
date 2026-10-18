package com.viernes.actividades.application.dto;

import com.viernes.actividades.domain.TipoActividad;
import com.viernes.actividades.domain.EstadoActividad;
import java.time.LocalDateTime;

public record ActividadResponse(
        Long id,
        String idUsuario,
        Long idCliente,
        Long idLead,
        Long idOportunidad,
        TipoActividad tipo,
        String titulo,
        String descripcion,
        LocalDateTime fecha,
        EstadoActividad estado,
        LocalDateTime creadoEn,
        boolean vencida
) {}
