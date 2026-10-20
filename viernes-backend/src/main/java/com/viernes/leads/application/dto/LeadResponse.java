package com.viernes.leads.application.dto;

import com.viernes.leads.domain.EstadoLead;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeadResponse(
    Long id,
    String nombre,
    String empresa,
    String correo,
    String telefono,
    String fuente,
    EstadoLead estado,
    String responsable,
    LocalDate fecha,
    LocalDateTime creadoEn,
    Long fuenteId,
    String fuenteCodigo,
    String fuenteNombre
) {}
