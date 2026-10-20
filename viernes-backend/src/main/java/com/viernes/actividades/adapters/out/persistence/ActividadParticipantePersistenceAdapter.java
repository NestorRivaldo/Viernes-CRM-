package com.viernes.actividades.adapters.out.persistence;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadParticipanteEntity;
import com.viernes.actividades.adapters.out.persistence.repository.ActividadParticipanteSpringDataRepository;
import com.viernes.actividades.application.port.out.ActividadParticipantePersistencePort;
import com.viernes.actividades.domain.ActividadParticipante;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ActividadParticipantePersistenceAdapter implements ActividadParticipantePersistencePort {

    private final ActividadParticipanteSpringDataRepository repository;

    public ActividadParticipantePersistenceAdapter(ActividadParticipanteSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public ActividadParticipante save(ActividadParticipante participante) {
        ActividadParticipanteEntity saved = repository.save(toEntity(participante));
        return toDomain(saved);
    }

    @Override
    public List<ActividadParticipante> findByIdActividad(Long idActividad) {
        return repository.findByIdActividad(idActividad).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<ActividadParticipante> findByIdActividadAndIdContacto(Long idActividad, Long idContacto) {
        return repository.findByIdActividadAndIdContacto(idActividad, idContacto).map(this::toDomain);
    }

    @Override
    public boolean existsByIdActividadAndIdContacto(Long idActividad, Long idContacto) {
        return repository.existsByIdActividadAndIdContacto(idActividad, idContacto);
    }

    @Override
    public void delete(ActividadParticipante participante) {
        repository.deleteById(participante.getId());
    }

    @Override
    public void deleteByIdActividad(Long idActividad) {
        repository.deleteByIdActividad(idActividad);
    }

    private ActividadParticipante toDomain(ActividadParticipanteEntity entity) {
        return new ActividadParticipante(
                entity.getId(),
                entity.getIdActividad(),
                entity.getIdContacto(),
                entity.getCreadoEn()
        );
    }

    private ActividadParticipanteEntity toEntity(ActividadParticipante domain) {
        ActividadParticipanteEntity entity = new ActividadParticipanteEntity();
        entity.setId(domain.getId());
        entity.setIdActividad(domain.getIdActividad());
        entity.setIdContacto(domain.getIdContacto());
        entity.setCreadoEn(domain.getCreadoEn());
        return entity;
    }
}
