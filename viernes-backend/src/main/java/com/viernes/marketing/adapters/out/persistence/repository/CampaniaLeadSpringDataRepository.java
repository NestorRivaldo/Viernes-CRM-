package com.viernes.marketing.adapters.out.persistence.repository;

import com.viernes.marketing.adapters.out.persistence.entity.CampaniaLeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaniaLeadSpringDataRepository extends JpaRepository<CampaniaLeadEntity, Long> {
    List<CampaniaLeadEntity> findByCampaniaId(Long campaniaId);
    boolean existsByCampaniaIdAndLeadId(Long campaniaId, Long leadId);
    void deleteByCampaniaIdAndLeadId(Long campaniaId, Long leadId);
    void deleteByCampaniaId(Long campaniaId);
}
