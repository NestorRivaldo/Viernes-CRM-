package com.viernes.oportunidades.application.port.in;

import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import java.util.List;

public interface ManageOportunidadUseCase {
    List<OportunidadResponse> listar();
    List<OportunidadResponse> buscar(String q);
    List<OportunidadResponse> filtrarPorEtapa(EtapaOportunidad etapa);
    OportunidadResponse obtener(Long id);
    OportunidadResponse crear(OportunidadRequest request);
    OportunidadResponse actualizar(Long id, OportunidadRequest request);
    OportunidadResponse cambiarEtapa(Long id, EtapaOportunidad etapa);
    void eliminar(Long id);
}
