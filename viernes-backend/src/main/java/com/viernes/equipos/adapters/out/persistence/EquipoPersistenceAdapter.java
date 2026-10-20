package com.viernes.equipos.adapters.out.persistence;

import com.viernes.equipos.adapters.out.persistence.entity.EquipoEntity;
import com.viernes.equipos.adapters.out.persistence.repository.EquipoSpringDataRepository;
import com.viernes.equipos.application.port.out.EquipoPersistencePort;
import com.viernes.equipos.domain.Equipo;
import com.viernes.equipos.domain.EstadoEquipo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EquipoPersistenceAdapter implements EquipoPersistencePort {

    private final EquipoSpringDataRepository repository;

    @Override
    public List<Equipo> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Equipo> findByEstado(EstadoEquipo estado) {
        return repository.findByEstado(estado).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Equipo> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return repository.existsByNombre(nombre);
    }

    @Override
    public Equipo save(Equipo equipo) {
        return toDomain(repository.save(toEntity(equipo)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Equipo toDomain(EquipoEntity e) {
        return Equipo.builder()
                .id(e.getId()).nombre(e.getNombre()).descripcion(e.getDescripcion())
                .estado(e.getEstado()).creadoEn(e.getCreadoEn())
                .build();
    }

    private EquipoEntity toEntity(Equipo e) {
        return EquipoEntity.builder()
                .id(e.getId()).nombre(e.getNombre()).descripcion(e.getDescripcion())
                .estado(e.getEstado()).creadoEn(e.getCreadoEn())
                .build();
    }
}
