package com.viernes.clinica.application.dto;

public record ClinicaResponse(
        Long id,
        String ruc,
        String razonSocial,
        String telefono,
        Boolean activo
) {
}
