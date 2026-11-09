package com.viernes.leads.adapters.out.persistence;

import com.viernes.leads.adapters.out.persistence.entity.FuenteEntity;
import com.viernes.leads.adapters.out.persistence.repository.FuenteSpringDataRepository;
import com.viernes.leads.application.port.out.FuentePersistencePort;
import com.viernes.leads.domain.Fuente;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FuentePersistenceAdapter implements FuentePersistencePort {

    private final FuenteSpringDataRepository repository;

    @Override
    public List<Fuente> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Fuente> findByActiva(Boolean activa) {
        return repository.findByActiva(activa).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Fuente> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return repository.existsByCodigo(codigo);
    }

    @Override
    public Fuente save(Fuente fuente) {
        return toDomain(repository.save(toEntity(fuente)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Fuente toDomain(FuenteEntity e) {
        return Fuente.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .activa(e.getActiva())
                .creadoEn(e.getCreadoEn())
                .build();
    }

    private FuenteEntity toEntity(Fuente f) {
        return FuenteEntity.builder()
                .id(f.getId())
                .codigo(f.getCodigo())
                .nombre(f.getNombre())
                .descripcion(f.getDescripcion())
                .activa(f.getActiva())
                .creadoEn(f.getCreadoEn())
                .build();
    }
}
