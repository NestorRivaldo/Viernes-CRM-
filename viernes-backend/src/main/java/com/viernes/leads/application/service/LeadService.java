package com.viernes.leads.application.service;

import com.viernes.leads.application.dto.LeadRequest;
import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.application.port.in.ManageLeadUseCase;
import com.viernes.leads.application.port.out.LeadPersistencePort;
import com.viernes.leads.domain.EstadoLead;
import com.viernes.leads.domain.Lead;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadService implements ManageLeadUseCase {

    private final LeadPersistencePort persistencePort;

    @Override
    public List<LeadResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<LeadResponse> buscar(String q) {
        return persistencePort.findByNombreOrEmpresa(q).stream().map(this::toResponse).toList();
    }

    @Override
    public List<LeadResponse> filtrarPorEstado(EstadoLead estado) {
        return persistencePort.findByEstado(estado).stream().map(this::toResponse).toList();
    }

    @Override
    public LeadResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public LeadResponse crear(LeadRequest request) {
        Lead l = Lead.builder()
            .creadoEn(LocalDateTime.now())
            .estado(request.estado() != null ? request.estado() : EstadoLead.NUEVO)
            .build();
        apply(l, request);
        return toResponse(persistencePort.save(l));
    }

    @Override
    @Transactional
    public LeadResponse actualizar(Long id, LeadRequest request) {
        Lead l = findOrThrow(id);
        apply(l, request);
        return toResponse(persistencePort.save(l));
    }

    @Override
    @Transactional
    public LeadResponse cambiarEstado(Long id, EstadoLead estado) {
        Lead l = findOrThrow(id);
        l.setEstado(estado);
        return toResponse(persistencePort.save(l));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        persistencePort.deleteById(id);
    }

    private Lead findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado: " + id));
    }

    private void apply(Lead l, LeadRequest r) {
        l.setNombre(r.nombre());
        l.setEmpresa(r.empresa());
        l.setCorreo(r.correo());
        l.setTelefono(r.telefono());
        l.setFuente(r.fuente());
        l.setResponsable(r.responsable());
        l.setFecha(r.fecha());
        if (r.estado() != null) l.setEstado(r.estado());
    }

    private LeadResponse toResponse(Lead l) {
        return new LeadResponse(l.getId(), l.getNombre(), l.getEmpresa(), l.getCorreo(),
                l.getTelefono(), l.getFuente(), l.getEstado(), l.getResponsable(),
                l.getFecha(), l.getCreadoEn());
    }
}
