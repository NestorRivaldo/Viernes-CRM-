package com.viernes.etiquetas.adapters.out.persistence;

import com.viernes.etiquetas.adapters.out.persistence.entity.EtiquetaEntity;
import com.viernes.etiquetas.adapters.out.persistence.repository.EtiquetaSpringDataRepository;
import com.viernes.etiquetas.application.port.out.EtiquetaPersistencePort;
import com.viernes.etiquetas.domain.Etiqueta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EtiquetaPersistenceAdapter implements EtiquetaPersistencePort {

    private final EtiquetaSpringDataRepository repository;

    @Override
    public List<Etiqueta> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Etiqueta> findByActiva(Boolean activa) {
        return repository.findByActiva(activa).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Etiqueta> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Etiqueta> findAllById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByNombreNormalizado(String nombreNormalizado) {
        return repository.existsByNombreNormalizado(nombreNormalizado);
    }

    @Override
    public boolean existsByNombreNormalizadoAndIdNot(String nombreNormalizado, Long id) {
        return repository.existsByNombreNormalizadoAndIdNot(nombreNormalizado, id);
    }

    @Override
    public Etiqueta save(Etiqueta etiqueta) {
        return toDomain(repository.save(toEntity(etiqueta)));
    }

    private Etiqueta toDomain(EtiquetaEntity entity) {
        return Etiqueta.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .nombreNormalizado(entity.getNombreNormalizado())
                .color(entity.getColor())
                .descripcion(entity.getDescripcion())
                .activa(entity.getActiva())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private EtiquetaEntity toEntity(Etiqueta etiqueta) {
        return EtiquetaEntity.builder()
                .id(etiqueta.getId())
                .nombre(etiqueta.getNombre())
                .nombreNormalizado(etiqueta.getNombreNormalizado())
                .color(etiqueta.getColor())
                .descripcion(etiqueta.getDescripcion())
                .activa(etiqueta.getActiva())
                .creadoEn(etiqueta.getCreadoEn())
                .build();
    }
}
