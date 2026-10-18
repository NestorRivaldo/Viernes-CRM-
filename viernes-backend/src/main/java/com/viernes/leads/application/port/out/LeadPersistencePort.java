package com.viernes.leads.application.port.out;

import com.viernes.leads.domain.EstadoLead;
import com.viernes.leads.domain.Lead;
import java.util.List;
import java.util.Optional;

public interface LeadPersistencePort {
    List<Lead> findAll();
    List<Lead> findByNombreOrEmpresa(String q);
    List<Lead> findByEstado(EstadoLead estado);
    List<Lead> findByResponsableIsNull();
    long count();
    long countByEstado(EstadoLead estado);
    Optional<Lead> findById(Long id);
    Lead save(Lead lead);
    void deleteById(Long id);
}
