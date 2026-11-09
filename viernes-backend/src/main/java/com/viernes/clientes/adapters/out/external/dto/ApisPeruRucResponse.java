package com.viernes.clientes.adapters.out.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApisPeruRucResponse(
        String ruc,
        String razonSocial,
        String nombreComercial,
        List<String> telefonos,
        String estado,
        String condicion,
        String direccion,
        String departamento,
        String provincia,
        String distrito,
        String ubigeo,
        String capital,
        Boolean success,
        String message
) {}
