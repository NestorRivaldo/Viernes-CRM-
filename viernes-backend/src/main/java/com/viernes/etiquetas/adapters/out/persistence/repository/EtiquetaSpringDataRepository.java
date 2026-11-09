package com.viernes.etiquetas.adapters.out.persistence.repository;

import com.viernes.etiquetas.adapters.out.persistence.entity.EtiquetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EtiquetaSpringDataRepository extends JpaRepository<EtiquetaEntity, Long> {
    List<EtiquetaEntity> findByActiva(Boolean activa);

    List<EtiquetaEntity> findByIdIn(Collection<Long> ids);

    boolean existsByNombreNormalizado(String nombreNormalizado);

    boolean existsByNombreNormalizadoAndIdNot(String nombreNormalizado, Long id);
}
