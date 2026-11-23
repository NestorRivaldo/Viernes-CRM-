package com.viernes.clinica.application.dto;

import com.viernes.clinica.domain.EstadoConsultorio;

import java.math.BigDecimal;
import java.util.List;

public record ConsultorioResponse(
        Long id,
        Long idClinica,
        String clinicaRazonSocial,
        String codigo,
        BigDecimal tarifaHora,
        EstadoConsultorio estado,
        List<EspecialidadResponse> especialidades
) {
}
