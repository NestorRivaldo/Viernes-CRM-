package com.viernes.marketing.adapters.out.persistence;

import com.viernes.marketing.adapters.out.persistence.entity.CampaniaEntity;
import com.viernes.marketing.adapters.out.persistence.repository.CampaniaSpringDataRepository;
import com.viernes.marketing.application.port.out.CampaniaPersistencePort;
import com.viernes.marketing.domain.Campania;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CampaniaPersistenceAdapter implements CampaniaPersistencePort {

    private final CampaniaSpringDataRepository repository;

    @Override
    public List<Campania> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Campania> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Campania save(Campania c) {
        return toDomain(repository.save(toEntity(c)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Campania toDomain(CampaniaEntity e) {
        return Campania.builder()
                .id(e.getId()).nombre(e.getNombre()).descripcion(e.getDescripcion())
                .fechaInicio(e.getFechaInicio()).fechaFin(e.getFechaFin())
                .estado(e.getEstado()).creadoEn(e.getCreadoEn())
                .build();
    }

    private CampaniaEntity toEntity(Campania c) {
        return CampaniaEntity.builder()
                .id(c.getId()).nombre(c.getNombre()).descripcion(c.getDescripcion())
                .fechaInicio(c.getFechaInicio()).fechaFin(c.getFechaFin())
                .estado(c.getEstado()).creadoEn(c.getCreadoEn())
                .build();
    }
}
