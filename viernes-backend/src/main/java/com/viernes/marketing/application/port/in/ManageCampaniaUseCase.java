package com.viernes.marketing.application.port.in;

import com.viernes.marketing.application.dto.CampaniaLeadResponse;
import com.viernes.marketing.application.dto.CampaniaRequest;
import com.viernes.marketing.application.dto.CampaniaResponse;
import com.viernes.marketing.domain.EstadoCampania;
import java.util.List;

public interface ManageCampaniaUseCase {
    List<CampaniaResponse> listar();
    CampaniaResponse obtener(Long id);
    CampaniaResponse crear(CampaniaRequest request);
    CampaniaResponse actualizar(Long id, CampaniaRequest request);
    CampaniaResponse cambiarEstado(Long id, EstadoCampania estado);
    void eliminar(Long id);
    CampaniaLeadResponse asociarLead(Long campaniaId, Long leadId);
    void desasociarLead(Long campaniaId, Long leadId);
    List<CampaniaLeadResponse> listarLeads(Long campaniaId);
}
