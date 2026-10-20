package com.viernes.actividades.application.port.out;

import com.viernes.actividades.domain.ActividadParticipante;
import java.util.List;
import java.util.Optional;

public interface ActividadParticipantePersistencePort {
    ActividadParticipante save(ActividadParticipante participante);
    List<ActividadParticipante> findByIdActividad(Long idActividad);
    Optional<ActividadParticipante> findByIdActividadAndIdContacto(Long idActividad, Long idContacto);
    boolean existsByIdActividadAndIdContacto(Long idActividad, Long idContacto);
    void delete(ActividadParticipante participante);
    void deleteByIdActividad(Long idActividad);
}
