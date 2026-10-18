package com.viernes.leads.adapters.out.persistence;

import com.viernes.leads.adapters.out.persistence.entity.LeadEntity;
import com.viernes.leads.adapters.out.persistence.repository.LeadSpringDataRepository;
import com.viernes.leads.application.port.out.LeadPersistencePort;
import com.viernes.leads.domain.EstadoLead;
import com.viernes.leads.domain.Lead;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeadPersistenceAdapter implements LeadPersistencePort {

    private final LeadSpringDataRepository repository;

    @Override
    public List<Lead> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Lead> findByNombreOrEmpresa(String q) {
        return repository.findByNombreContainingIgnoreCaseOrEmpresaContainingIgnoreCase(q, q)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Lead> findByEstado(EstadoLead estado) {
        return repository.findByEstado(estado).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Lead> findByResponsableIsNull() {
        return repository.findByResponsableIsNull().stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByEstado(EstadoLead estado) {
        return repository.countByEstado(estado);
    }

    @Override
    public Optional<Lead> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Lead save(Lead lead) {
        LeadEntity entity = toEntity(lead);
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Lead toDomain(LeadEntity entity) {
        return Lead.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .empresa(entity.getEmpresa())
                .correo(entity.getCorreo())
                .telefono(entity.getTelefono())
                .fuente(entity.getFuente())
                .estado(entity.getEstado())
                .responsable(entity.getResponsable())
                .fecha(entity.getFecha())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private LeadEntity toEntity(Lead domain) {
        return LeadEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .empresa(domain.getEmpresa())
                .correo(domain.getCorreo())
                .telefono(domain.getTelefono())
                .fuente(domain.getFuente())
                .estado(domain.getEstado())
                .responsable(domain.getResponsable())
                .fecha(domain.getFecha())
                .creadoEn(domain.getCreadoEn())
                .build();
    }
}
