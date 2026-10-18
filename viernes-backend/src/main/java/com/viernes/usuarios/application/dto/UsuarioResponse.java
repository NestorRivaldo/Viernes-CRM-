package com.viernes.usuarios.application.dto;

import com.viernes.usuarios.domain.EstadoUsuario;
import com.viernes.usuarios.domain.RolUsuario;
import java.time.LocalDateTime;

public record UsuarioResponse(
    Long id,
    String nombre,
    String apellido,
    String correo,
    RolUsuario rol,
    EstadoUsuario estado,
    LocalDateTime creadoEn
) {}
