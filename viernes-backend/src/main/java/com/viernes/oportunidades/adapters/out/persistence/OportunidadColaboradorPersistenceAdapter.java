package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadColaboradorEntity;
import com.viernes.oportunidades.adapters.out.persistence.repository.OportunidadColaboradorSpringDataRepository;
import com.viernes.oportunidades.application.port.out.OportunidadColaboradorPersistencePort;
import com.viernes.oportunidades.domain.OportunidadColaborador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OportunidadColaboradorPersistenceAdapter implements OportunidadColaboradorPersistencePort {

    private final OportunidadColaboradorSpringDataRepository repository;

    @Override
    public OportunidadColaborador save(OportunidadColaborador colaborador) {
        return toDomain(repository.save(toEntity(colaborador)));
    }

    @Override
    public List<OportunidadColaborador> findByIdOportunidad(Long idOportunidad) {
        return repository.findByIdOportunidad(idOportunidad).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<OportunidadColaborador> findByIdOportunidadAndIdUsuario(Long idOportunidad, Long idUsuario) {
        return repository.findByIdOportunidadAndIdUsuario(idOportunidad, idUsuario).map(this::toDomain);
    }

    @Override
    public boolean existsByIdOportunidadAndIdUsuario(Long idOportunidad, Long idUsuario) {
        return repository.existsByIdOportunidadAndIdUsuario(idOportunidad, idUsuario);
    }

    @Override
    public void delete(OportunidadColaborador colaborador) {
        repository.deleteById(colaborador.getId());
    }

    @Override
    public void deleteByIdOportunidad(Long idOportunidad) {
        repository.deleteByIdOportunidad(idOportunidad);
    }

    private OportunidadColaborador toDomain(OportunidadColaboradorEntity entity) {
        return OportunidadColaborador.builder()
                .id(entity.getId())
                .idOportunidad(entity.getIdOportunidad())
                .idUsuario(entity.getIdUsuario())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private OportunidadColaboradorEntity toEntity(OportunidadColaborador domain) {
        return OportunidadColaboradorEntity.builder()
                .id(domain.getId())
                .idOportunidad(domain.getIdOportunidad())
                .idUsuario(domain.getIdUsuario())
                .creadoEn(domain.getCreadoEn())
                .build();
    }
}
