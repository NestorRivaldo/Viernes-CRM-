package com.viernes.mensajeria.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensajeRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String contenido
) {}
