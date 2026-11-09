package com.viernes.etiquetas.application.port.in;

import com.viernes.etiquetas.application.dto.EtiquetaRequest;
import com.viernes.etiquetas.application.dto.EtiquetaResponse;

import java.util.List;

public interface ManageEtiquetaUseCase {
    List<EtiquetaResponse> listar(Boolean activa, String q);

    EtiquetaResponse obtener(Long id);

    EtiquetaResponse crear(EtiquetaRequest request);

    EtiquetaResponse actualizar(Long id, EtiquetaRequest request);

    void eliminar(Long id);
}
