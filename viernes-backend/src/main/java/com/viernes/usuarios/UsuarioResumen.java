package com.viernes.usuarios;

import java.time.LocalDateTime;

/**
 * Vista pública de un usuario para consumo inter-módulo.
 * {@code rol} y {@code estado} viajan como String para no exponer los enums
 * del dominio interno. {@code passwordHash} solo lo utiliza el módulo auth
 * para verificar credenciales; nunca debe serializarse hacia clientes.
 */
public record UsuarioResumen(
    Long id,
    String nombre,
    String apellido,
    String correo,
    String passwordHash,
    String rol,
    String estado,
    LocalDateTime creadoEn
) {}
