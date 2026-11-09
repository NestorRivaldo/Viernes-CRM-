package com.viernes.actividades.adapters.out.persistence.repository;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadEtiquetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ActividadEtiquetaSpringDataRepository extends JpaRepository<ActividadEtiquetaEntity, Long> {
    List<ActividadEtiquetaEntity> findByIdActividadOrderByOrdenAsc(Long idActividad);

    List<ActividadEtiquetaEntity> findByIdActividadInOrderByIdActividadAscOrdenAsc(Collection<Long> idsActividad);

    void deleteByIdActividad(Long idActividad);
}
