package com.viernes.actividades.adapters.out.persistence.repository;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadParticipanteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActividadParticipanteSpringDataRepository extends JpaRepository<ActividadParticipanteEntity, Long> {
    List<ActividadParticipanteEntity> findByIdActividad(Long idActividad);
    Optional<ActividadParticipanteEntity> findByIdActividadAndIdContacto(Long idActividad, Long idContacto);
    boolean existsByIdActividadAndIdContacto(Long idActividad, Long idContacto);
    void deleteByIdActividad(Long idActividad);
}
