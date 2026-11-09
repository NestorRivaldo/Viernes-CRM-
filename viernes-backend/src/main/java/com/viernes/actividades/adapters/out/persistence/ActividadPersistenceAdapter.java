package com.viernes.actividades.adapters.out.persistence;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadEntity;
import com.viernes.actividades.adapters.out.persistence.repository.ActividadSpringDataRepository;
import com.viernes.actividades.application.port.out.ActividadPersistencePort;
import com.viernes.actividades.domain.Actividad;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ActividadPersistenceAdapter implements ActividadPersistencePort {

    private final ActividadSpringDataRepository repository;

    public ActividadPersistenceAdapter(ActividadSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Actividad save(Actividad actividad) {
        ActividadEntity entity = toEntity(actividad);
        ActividadEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Actividad> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Actividad> findAll() {
        // Orden explícito: sin ORDER BY, Oracle devuelve las filas en orden de
        // almacenamiento y cualquier UPDATE puede alterarlo. Se ordena por ID
        // descendente (creación más reciente primero), de modo que una
        // actividad recién creada aparezca en la primera fila.
        Sort orden = Sort.by(Sort.Direction.DESC, "id");
        return repository.findAll(orden).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByEstado(EstadoActividad estado) {
        return repository.findByEstado(estado).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByIdCliente(Long idCliente) {
        return repository.findByIdCliente(idCliente).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByIdLead(Long idLead) {
        return repository.findByIdLead(idLead).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByIdOportunidad(Long idOportunidad) {
        return repository.findByIdOportunidad(idOportunidad).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByEtiquetaId(Long idEtiqueta) {
        return repository.findByEtiquetaId(idEtiqueta).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByIdUsuario(String idUsuario) {
        return repository.findByIdUsuario(idUsuario).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findByTipo(TipoActividad tipo) {
        return repository.findByTipo(tipo).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Actividad> findVencidas(EstadoActividad estado, java.time.LocalDateTime ahora) {
        return repository.findVencidas(estado, ahora).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Actividad toDomain(ActividadEntity entity) {
        return new Actividad(
                entity.getId(),
                entity.getIdUsuario(),
                entity.getIdCliente(),
                entity.getIdLead(),
                entity.getIdOportunidad(),
                entity.getTipo(),
                entity.getTitulo(),
                entity.getDescripcion(),
                entity.getFecha(),
                entity.getEstado(),
                entity.getCreadoEn()
        );
    }

    private ActividadEntity toEntity(Actividad domain) {
        ActividadEntity entity = new ActividadEntity();
        entity.setId(domain.getId());
        entity.setIdUsuario(domain.getIdUsuario());
        entity.setIdCliente(domain.getIdCliente());
        entity.setIdLead(domain.getIdLead());
        entity.setIdOportunidad(domain.getIdOportunidad());
        entity.setTipo(domain.getTipo());
        entity.setTitulo(domain.getTitulo());
        entity.setDescripcion(domain.getDescripcion());
        entity.setFecha(domain.getFecha());
        entity.setEstado(domain.getEstado());
        entity.setCreadoEn(domain.getCreadoEn());
        return entity;
    }
}
