package com.viernes.clientes.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactoRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    String nombre,
    
    @Size(max = 100)
    String apellido,
    
    @Size(max = 100)
    String cargo,
    
    @Size(max = 150)
    String correo,
    
    @Size(max = 30)
    String telefono,
    
    Boolean principal
) {}
