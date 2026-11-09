package com.viernes.clientes.adapters.out.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApisPeruDniResponse(
        String dni,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String codVerifica,
        Boolean success,
        String message
) {}
