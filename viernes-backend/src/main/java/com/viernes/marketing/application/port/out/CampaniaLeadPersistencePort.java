package com.viernes.marketing.application.port.out;

import com.viernes.marketing.domain.CampaniaLead;
import java.util.List;

public interface CampaniaLeadPersistencePort {
    List<CampaniaLead> findByCampaniaId(Long campaniaId);
    boolean existsByCampaniaIdAndLeadId(Long campaniaId, Long leadId);
    CampaniaLead save(CampaniaLead campaniaLead);
    void deleteByCampaniaIdAndLeadId(Long campaniaId, Long leadId);
    void deleteByCampaniaId(Long campaniaId);
}
