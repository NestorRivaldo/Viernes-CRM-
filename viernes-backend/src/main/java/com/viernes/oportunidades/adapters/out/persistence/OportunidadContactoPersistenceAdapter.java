package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadContactoEntity;
import com.viernes.oportunidades.adapters.out.persistence.repository.OportunidadContactoSpringDataRepository;
import com.viernes.oportunidades.application.port.out.OportunidadContactoPersistencePort;
import com.viernes.oportunidades.domain.OportunidadContacto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OportunidadContactoPersistenceAdapter implements OportunidadContactoPersistencePort {

    private final OportunidadContactoSpringDataRepository repository;

    @Override
    public OportunidadContacto save(OportunidadContacto contacto) {
        return toDomain(repository.save(toEntity(contacto)));
    }

    @Override
    public List<OportunidadContacto> findByIdOportunidad(Long idOportunidad) {
        return repository.findByIdOportunidad(idOportunidad).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<OportunidadContacto> findByIdOportunidadAndIdContacto(Long idOportunidad, Long idContacto) {
        return repository.findByIdOportunidadAndIdContacto(idOportunidad, idContacto).map(this::toDomain);
    }

    @Override
    public boolean existsByIdOportunidadAndIdContacto(Long idOportunidad, Long idContacto) {
        return repository.existsByIdOportunidadAndIdContacto(idOportunidad, idContacto);
    }

    @Override
    public void delete(OportunidadContacto contacto) {
        repository.deleteById(contacto.getId());
    }

    @Override
    public void deleteByIdOportunidad(Long idOportunidad) {
        repository.deleteByIdOportunidad(idOportunidad);
    }

    private OportunidadContacto toDomain(OportunidadContactoEntity entity) {
        return OportunidadContacto.builder()
                .id(entity.getId())
                .idOportunidad(entity.getIdOportunidad())
                .idContacto(entity.getIdContacto())
                .rol(entity.getRol())
                .esDecisor(entity.getEsDecisor())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private OportunidadContactoEntity toEntity(OportunidadContacto domain) {
        return OportunidadContactoEntity.builder()
                .id(domain.getId())
                .idOportunidad(domain.getIdOportunidad())
                .idContacto(domain.getIdContacto())
                .rol(domain.getRol())
                .esDecisor(domain.getEsDecisor() != null ? domain.getEsDecisor() : false)
                .creadoEn(domain.getCreadoEn())
                .build();
    }
}
