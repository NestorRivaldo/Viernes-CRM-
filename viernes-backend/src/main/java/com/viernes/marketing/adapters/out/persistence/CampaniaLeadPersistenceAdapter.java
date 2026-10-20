package com.viernes.marketing.adapters.out.persistence;

import com.viernes.marketing.adapters.out.persistence.entity.CampaniaLeadEntity;
import com.viernes.marketing.adapters.out.persistence.repository.CampaniaLeadSpringDataRepository;
import com.viernes.marketing.application.port.out.CampaniaLeadPersistencePort;
import com.viernes.marketing.domain.CampaniaLead;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CampaniaLeadPersistenceAdapter implements CampaniaLeadPersistencePort {

    private final CampaniaLeadSpringDataRepository repository;

    @Override
    public List<CampaniaLead> findByCampaniaId(Long campaniaId) {
        return repository.findByCampaniaId(campaniaId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByCampaniaIdAndLeadId(Long campaniaId, Long leadId) {
        return repository.existsByCampaniaIdAndLeadId(campaniaId, leadId);
    }

    @Override
    public CampaniaLead save(CampaniaLead campaniaLead) {
        return toDomain(repository.save(toEntity(campaniaLead)));
    }

    @Override
    public void deleteByCampaniaIdAndLeadId(Long campaniaId, Long leadId) {
        repository.deleteByCampaniaIdAndLeadId(campaniaId, leadId);
    }

    @Override
    public void deleteByCampaniaId(Long campaniaId) {
        repository.deleteByCampaniaId(campaniaId);
    }

    private CampaniaLead toDomain(CampaniaLeadEntity e) {
        return CampaniaLead.builder()
                .id(e.getId()).campaniaId(e.getCampaniaId())
                .leadId(e.getLeadId()).creadoEn(e.getCreadoEn())
                .build();
    }

    private CampaniaLeadEntity toEntity(CampaniaLead c) {
        return CampaniaLeadEntity.builder()
                .id(c.getId()).campaniaId(c.getCampaniaId())
                .leadId(c.getLeadId()).creadoEn(c.getCreadoEn())
                .build();
    }
}
