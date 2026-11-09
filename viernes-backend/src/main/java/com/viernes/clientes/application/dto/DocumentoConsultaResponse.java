package com.viernes.clientes.application.dto;

import com.viernes.clientes.domain.TipoDocumento;
import java.util.List;

public record DocumentoConsultaResponse(
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombreCompleto,
        String razonSocial,
        String nombreComercial,
        String estado,
        String condicion,
        String direccion,
        List<String> telefonos
) {}
