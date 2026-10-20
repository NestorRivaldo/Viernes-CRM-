package com.viernes.leads.adapters.out.persistence.repository;

import com.viernes.leads.adapters.out.persistence.entity.FuenteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuenteSpringDataRepository extends JpaRepository<FuenteEntity, Long> {
    List<FuenteEntity> findByActiva(Boolean activa);
    boolean existsByCodigo(String codigo);
}
