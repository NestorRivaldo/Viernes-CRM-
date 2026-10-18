package com.viernes.usuarios.application.dto;

import com.viernes.usuarios.domain.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    String apellido,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe ser válido")
    @Size(max = 150)
    String correo,

    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    String password,

    @NotNull(message = "El rol es obligatorio")
    RolUsuario rol
) {}
