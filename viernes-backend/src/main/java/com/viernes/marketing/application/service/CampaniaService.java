package com.viernes.marketing.application.service;

import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.application.port.in.ManageLeadUseCase;
import com.viernes.marketing.application.dto.CampaniaLeadResponse;
import com.viernes.marketing.application.dto.CampaniaRequest;
import com.viernes.marketing.application.dto.CampaniaResponse;
import com.viernes.marketing.application.port.in.ManageCampaniaUseCase;
import com.viernes.marketing.application.port.out.CampaniaLeadPersistencePort;
import com.viernes.marketing.application.port.out.CampaniaPersistencePort;
import com.viernes.marketing.domain.Campania;
import com.viernes.marketing.domain.CampaniaLead;
import com.viernes.marketing.domain.EstadoCampania;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaniaService implements ManageCampaniaUseCase {

    private final CampaniaPersistencePort persistencePort;
    private final CampaniaLeadPersistencePort campaniaLeadPort;
    private final ManageLeadUseCase leadUseCase;

    @Override
    public List<CampaniaResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public CampaniaResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public CampaniaResponse crear(CampaniaRequest request) {
        Campania c = Campania.builder()
            .creadoEn(LocalDateTime.now())
            .estado(request.estado() != null ? request.estado() : EstadoCampania.BORRADOR)
            .build();
        apply(c, request);
        return toResponse(persistencePort.save(c));
    }

    @Override
    @Transactional
    public CampaniaResponse actualizar(Long id, CampaniaRequest request) {
        Campania c = findOrThrow(id);
        apply(c, request);
        return toResponse(persistencePort.save(c));
    }

    @Override
    @Transactional
    public CampaniaResponse cambiarEstado(Long id, EstadoCampania estado) {
        Campania c = findOrThrow(id);
        c.setEstado(estado);
        return toResponse(persistencePort.save(c));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        campaniaLeadPort.deleteByCampaniaId(id);
        persistencePort.deleteById(id);
    }

    @Override
    @Transactional
    public CampaniaLeadResponse asociarLead(Long campaniaId, Long leadId) {
        findOrThrow(campaniaId);
        LeadResponse lead = leadUseCase.obtener(leadId);
        if (campaniaLeadPort.existsByCampaniaIdAndLeadId(campaniaId, leadId)) {
            throw new IllegalArgumentException(
                    "El lead " + leadId + " ya está asociado a la campaña: " + campaniaId);
        }
        CampaniaLead guardado = campaniaLeadPort.save(CampaniaLead.builder()
                .campaniaId(campaniaId)
                .leadId(leadId)
                .creadoEn(LocalDateTime.now())
                .build());
        return toLeadResponse(guardado, lead);
    }

    @Override
    @Transactional
    public void desasociarLead(Long campaniaId, Long leadId) {
        findOrThrow(campaniaId);
        if (!campaniaLeadPort.existsByCampaniaIdAndLeadId(campaniaId, leadId)) {
            throw new ResourceNotFoundException(
                    "El lead " + leadId + " no está asociado a la campaña: " + campaniaId);
        }
        campaniaLeadPort.deleteByCampaniaIdAndLeadId(campaniaId, leadId);
    }

    @Override
    public List<CampaniaLeadResponse> listarLeads(Long campaniaId) {
        findOrThrow(campaniaId);
        return campaniaLeadPort.findByCampaniaId(campaniaId).stream()
                .map(this::enriquecerConLead)
                .toList();
    }

    private Campania findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada: " + id));
    }

    private CampaniaLeadResponse enriquecerConLead(CampaniaLead asociacion) {
        LeadResponse lead = null;
        try {
            lead = leadUseCase.obtener(asociacion.getLeadId());
        } catch (ResourceNotFoundException e) {
            // Referencia lógica sin FK: el lead pudo eliminarse; se devuelve la asociación sin datos
        }
        return toLeadResponse(asociacion, lead);
    }

    private void apply(Campania c, CampaniaRequest r) {
        c.setNombre(r.nombre());
        c.setDescripcion(r.descripcion());
        c.setFechaInicio(r.fechaInicio());
        c.setFechaFin(r.fechaFin());
        if (r.estado() != null) c.setEstado(r.estado());
    }

    private CampaniaResponse toResponse(Campania c) {
        return new CampaniaResponse(c.getId(), c.getNombre(), c.getDescripcion(),
                c.getFechaInicio(), c.getFechaFin(), c.getEstado(), c.getCreadoEn());
    }

    private CampaniaLeadResponse toLeadResponse(CampaniaLead c, LeadResponse lead) {
        return new CampaniaLeadResponse(c.getId(), c.getCampaniaId(), c.getLeadId(),
                lead != null ? lead.nombre() : null,
                lead != null ? lead.empresa() : null,
                lead != null ? lead.correo() : null,
                lead != null ? lead.estado() : null,
                c.getCreadoEn());
    }
}
