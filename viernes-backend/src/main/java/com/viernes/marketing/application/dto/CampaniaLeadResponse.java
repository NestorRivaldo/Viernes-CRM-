package com.viernes.marketing.application.dto;

import com.viernes.leads.domain.EstadoLead;
import java.time.LocalDateTime;

public record CampaniaLeadResponse(
    Long id,
    Long campaniaId,
    Long leadId,
    String leadNombre,
    String leadEmpresa,
    String leadCorreo,
    EstadoLead leadEstado,
    LocalDateTime creadoEn
) {}
