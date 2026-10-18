package com.viernes.auth.application.dto;

import java.time.LocalDateTime;

public record UsuarioLoginResponse(
    Long id,
    String nombre,
    String apellido,
    String correo,
    String rol,
    String estado,
    LocalDateTime creadoEn
) {}
