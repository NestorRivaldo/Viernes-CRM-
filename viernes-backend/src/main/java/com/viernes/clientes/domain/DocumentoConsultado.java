package com.viernes.clientes.domain;

import java.util.List;

/**
 * Datos de un documento consultado en una fuente externa (RENIEC/SUNAT).
 * Es un modelo de dominio, no la respuesta cruda del proveedor.
 */
public record DocumentoConsultado(
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
