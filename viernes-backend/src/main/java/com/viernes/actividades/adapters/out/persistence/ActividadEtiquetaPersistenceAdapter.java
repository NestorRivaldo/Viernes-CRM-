package com.viernes.actividades.adapters.out.persistence;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadEtiquetaEntity;
import com.viernes.actividades.adapters.out.persistence.repository.ActividadEtiquetaSpringDataRepository;
import com.viernes.actividades.application.port.out.ActividadEtiquetaPersistencePort;
import com.viernes.actividades.domain.ActividadEtiqueta;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ActividadEtiquetaPersistenceAdapter implements ActividadEtiquetaPersistencePort {

    private final ActividadEtiquetaSpringDataRepository repository;

    public ActividadEtiquetaPersistenceAdapter(ActividadEtiquetaSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ActividadEtiqueta> findByIdActividad(Long idActividad) {
        return repository.findByIdActividadOrderByOrdenAsc(idActividad).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActividadEtiqueta> findByIdActividadIn(Collection<Long> idsActividad) {
        if (idsActividad == null || idsActividad.isEmpty()) {
            return List.of();
        }
        return repository.findByIdActividadInOrderByIdActividadAscOrdenAsc(idsActividad).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void reemplazarEtiquetas(Long idActividad, Collection<Long> idsEtiqueta) {
        repository.deleteByIdActividad(idActividad);
        repository.flush();

        if (idsEtiqueta == null || idsEtiqueta.isEmpty()) {
            return;
        }

        List<Long> ordenados = idsEtiqueta.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<ActividadEtiquetaEntity> nuevas = new ArrayList<>(ordenados.size());
        for (int i = 0; i < ordenados.size(); i++) {
            nuevas.add(toEntity(new ActividadEtiqueta(
                    null, idActividad, ordenados.get(i), LocalDateTime.now(), i)));
        }

        repository.saveAll(nuevas);
    }

    @Override
    public void deleteByIdActividad(Long idActividad) {
        repository.deleteByIdActividad(idActividad);
    }

    private ActividadEtiqueta toDomain(ActividadEtiquetaEntity entity) {
        return new ActividadEtiqueta(
                entity.getId(),
                entity.getIdActividad(),
                entity.getIdEtiqueta(),
                entity.getCreadoEn(),
                entity.getOrden()
        );
    }

    private ActividadEtiquetaEntity toEntity(ActividadEtiqueta domain) {
        ActividadEtiquetaEntity entity = new ActividadEtiquetaEntity();
        entity.setId(domain.getId());
        entity.setIdActividad(domain.getIdActividad());
        entity.setIdEtiqueta(domain.getIdEtiqueta());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setOrden(domain.getOrden());
        return entity;
    }
}
