package com.viernes.etiquetas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EtiquetaRequest(
        @NotBlank(message = "El nombre de la etiqueta es obligatorio")
        @Size(max = 40, message = "El nombre no puede superar los 40 caracteres")
        String nombre,

        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "El color debe tener formato hexadecimal #RRGGBB")
        String color,

        @Size(max = 160, message = "La descripción no puede superar los 160 caracteres")
        String descripcion,

        Boolean activa
) {
}
