package com.viernes.clientes.application.dto;

import com.viernes.clientes.domain.EstadoCliente;
import com.viernes.clientes.domain.TipoDocumento;
import java.time.LocalDateTime;
import java.util.List;

public record ClienteResponse(
    Long id,
    String nombre,
    String empresa,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String correo,
    String telefono,
    EstadoCliente estado,
    String responsable,
    LocalDateTime creadoEn,
    List<ContactoResponse> contactos
) {}
