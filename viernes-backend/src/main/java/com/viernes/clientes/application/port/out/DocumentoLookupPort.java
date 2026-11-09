package com.viernes.clientes.application.port.out;

import com.viernes.clientes.domain.DocumentoConsultado;
import com.viernes.clientes.domain.TipoDocumento;

/**
 * Puerto de salida para consultar documentos en una fuente externa.
 * La implementacion concreta decide el proveedor (APIsPERU, etc.).
 */
public interface DocumentoLookupPort {
    DocumentoConsultado consultar(TipoDocumento tipoDocumento, String numeroDocumento);
}
