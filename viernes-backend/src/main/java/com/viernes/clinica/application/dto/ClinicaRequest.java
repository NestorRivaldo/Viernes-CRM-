package com.viernes.clinica.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClinicaRequest(
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "^\\d{11}$", message = "El RUC debe tener 11 dígitos")
        String ruc,

        @NotBlank(message = "La razón social es obligatoria")
        String razonSocial,

        String telefono,

        Boolean activo
) {
}
