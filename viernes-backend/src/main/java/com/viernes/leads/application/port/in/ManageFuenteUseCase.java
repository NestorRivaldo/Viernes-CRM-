package com.viernes.leads.application.port.in;

import com.viernes.leads.application.dto.FuenteRequest;
import com.viernes.leads.application.dto.FuenteResponse;
import java.util.List;

public interface ManageFuenteUseCase {
    List<FuenteResponse> listar(Boolean activa);
    FuenteResponse obtener(Long id);
    FuenteResponse crear(FuenteRequest request);
    FuenteResponse actualizar(Long id, FuenteRequest request);
    void eliminar(Long id);
}
