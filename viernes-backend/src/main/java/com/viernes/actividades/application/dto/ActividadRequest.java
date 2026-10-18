package com.viernes.actividades.application.dto;

import com.viernes.actividades.domain.TipoActividad;
import com.viernes.actividades.domain.EstadoActividad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

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
        
        EstadoActividad estado
) {}
