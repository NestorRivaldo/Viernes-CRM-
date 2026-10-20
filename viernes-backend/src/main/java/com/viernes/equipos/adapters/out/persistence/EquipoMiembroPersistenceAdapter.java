package com.viernes.equipos.adapters.out.persistence;

import com.viernes.equipos.adapters.out.persistence.entity.EquipoMiembroEntity;
import com.viernes.equipos.adapters.out.persistence.repository.EquipoMiembroSpringDataRepository;
import com.viernes.equipos.application.port.out.EquipoMiembroPersistencePort;
import com.viernes.equipos.domain.EquipoMiembro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EquipoMiembroPersistenceAdapter implements EquipoMiembroPersistencePort {

    private final EquipoMiembroSpringDataRepository repository;

    @Override
    public List<EquipoMiembro> findByEquipoId(Long equipoId) {
        return repository.findByEquipoId(equipoId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId) {
        return repository.existsByEquipoIdAndUsuarioId(equipoId, usuarioId);
    }

    @Override
    public long countByEquipoId(Long equipoId) {
        return repository.countByEquipoId(equipoId);
    }

    @Override
    public EquipoMiembro save(EquipoMiembro miembro) {
        return toDomain(repository.save(toEntity(miembro)));
    }

    @Override
    public void deleteByEquipoId(Long equipoId) {
        repository.deleteByEquipoId(equipoId);
    }

    @Override
    public void deleteByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId) {
        repository.deleteByEquipoIdAndUsuarioId(equipoId, usuarioId);
    }

    private EquipoMiembro toDomain(EquipoMiembroEntity e) {
        return EquipoMiembro.builder()
                .id(e.getId()).equipoId(e.getEquipoId()).usuarioId(e.getUsuarioId())
                .creadoEn(e.getCreadoEn())
                .build();
    }

    private EquipoMiembroEntity toEntity(EquipoMiembro m) {
        return EquipoMiembroEntity.builder()
                .id(m.getId()).equipoId(m.getEquipoId()).usuarioId(m.getUsuarioId())
                .creadoEn(m.getCreadoEn())
                .build();
    }
}
