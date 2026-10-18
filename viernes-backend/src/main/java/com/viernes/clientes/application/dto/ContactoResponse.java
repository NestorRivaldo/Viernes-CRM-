package com.viernes.clientes.application.dto;

import java.time.LocalDateTime;

public record ContactoResponse(
    Long id,
    Long idCliente,
    String nombre,
    String apellido,
    String cargo,
    String correo,
    String telefono,
    Boolean principal,
    LocalDateTime creadoEn
) {}
