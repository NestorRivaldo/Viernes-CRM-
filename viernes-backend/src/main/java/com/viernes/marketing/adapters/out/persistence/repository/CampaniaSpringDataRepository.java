package com.viernes.marketing.adapters.out.persistence.repository;

import com.viernes.marketing.adapters.out.persistence.entity.CampaniaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaniaSpringDataRepository extends JpaRepository<CampaniaEntity, Long> {
}
