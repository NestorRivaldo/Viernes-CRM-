package com.viernes.actividades.application.dto;

import com.viernes.actividades.domain.TipoActividad;
import com.viernes.actividades.domain.EstadoActividad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ActividadRequest(
        String idUsuario,
        Long idCliente,
        Long idLead,
        Long idOportunidad,

        @NotNull(message = "El tipo de actividad es obligatorio")
        TipoActividad tipo,

        @NotBlank(message = "El título es obligatorio")
        String titulo,

        String descripcion,

        @NotNull(message = "La fecha es obligatoria")
        LocalDateTime fecha,

        EstadoActividad estado,

        @Size(max = 8, message = "Una actividad puede tener como máximo 8 etiquetas")
        List<@Positive(message = "El ID de etiqueta es inválido") Long> etiquetaIds
) {
    /**
     * Constructor de compatibilidad para clientes anteriores a las etiquetas.
     */
    public ActividadRequest(String idUsuario, Long idCliente, Long idLead, Long idOportunidad,
                            TipoActividad tipo, String titulo, String descripcion, LocalDateTime fecha,
                            EstadoActividad estado) {
        this(idUsuario, idCliente, idLead, idOportunidad, tipo, titulo, descripcion, fecha, estado, List.of());
    }
}
