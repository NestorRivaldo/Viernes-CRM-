package com.viernes.leads.adapters.out.persistence.repository;

import com.viernes.leads.adapters.out.persistence.entity.LeadEntity;
import com.viernes.leads.domain.EstadoLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeadSpringDataRepository extends JpaRepository<LeadEntity, Long> {
    List<LeadEntity> findByEstado(EstadoLead estado);
    List<LeadEntity> findByNombreContainingIgnoreCaseOrEmpresaContainingIgnoreCase(String nombre, String empresa);
    long countByEstado(EstadoLead estado);
    List<LeadEntity> findByResponsableIsNull();
}
