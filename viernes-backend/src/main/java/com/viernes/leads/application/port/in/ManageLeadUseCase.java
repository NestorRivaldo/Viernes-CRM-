package com.viernes.leads.application.port.in;

import com.viernes.leads.application.dto.LeadRequest;
import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.domain.EstadoLead;
import java.util.List;

public interface ManageLeadUseCase {
    List<LeadResponse> listar();
    List<LeadResponse> buscar(String q);
    List<LeadResponse> filtrarPorEstado(EstadoLead estado);
    LeadResponse obtener(Long id);
    LeadResponse crear(LeadRequest request);
    LeadResponse actualizar(Long id, LeadRequest request);
    LeadResponse cambiarEstado(Long id, EstadoLead estado);
    void eliminar(Long id);
}
