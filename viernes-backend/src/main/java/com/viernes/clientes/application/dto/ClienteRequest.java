package com.viernes.clientes.application.dto;

import com.viernes.clientes.domain.EstadoCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 150) String empresa,
    @Email @Size(max = 150) String correo,
    @Size(max = 30) String telefono,
    EstadoCliente estado,
    @Size(max = 100) String responsable
) {}
