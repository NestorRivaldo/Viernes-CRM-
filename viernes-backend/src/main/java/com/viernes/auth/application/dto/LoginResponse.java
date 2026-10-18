package com.viernes.auth.application.dto;

public record LoginResponse(
    String token,
    String tipo,
    long expiresIn,
    UsuarioLoginResponse usuario
) {}
