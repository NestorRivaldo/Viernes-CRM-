package com.viernes.actividades.application.dto;

import com.viernes.actividades.domain.TipoActividad;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.etiquetas.application.dto.EtiquetaResponse;

import java.time.LocalDateTime;
import java.util.List;

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
        boolean vencida,
        List<EtiquetaResponse> etiquetas
) {
    /**
     * Constructor de compatibilidad para pruebas y clientes que aún no leen etiquetas.
     */
    public ActividadResponse(Long id, String idUsuario, Long idCliente, Long idLead, Long idOportunidad,
                             TipoActividad tipo, String titulo, String descripcion, LocalDateTime fecha,
                             EstadoActividad estado, LocalDateTime creadoEn, boolean vencida) {
        this(id, idUsuario, idCliente, idLead, idOportunidad, tipo, titulo, descripcion, fecha,
                estado, creadoEn, vencida, List.of());
    }
}
