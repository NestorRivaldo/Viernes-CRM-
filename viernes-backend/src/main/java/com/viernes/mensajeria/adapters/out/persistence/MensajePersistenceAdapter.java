package com.viernes.mensajeria.adapters.out.persistence;

import com.viernes.mensajeria.adapters.out.persistence.entity.MensajeEntity;
import com.viernes.mensajeria.adapters.out.persistence.repository.MensajeSpringDataRepository;
import com.viernes.mensajeria.application.port.out.MensajePersistencePort;
import com.viernes.mensajeria.domain.Mensaje;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MensajePersistenceAdapter implements MensajePersistencePort {

    private final MensajeSpringDataRepository repository;

    @Override
    public List<Mensaje> findRecientes(int limite) {
        return repository.findUltimos(PageRequest.of(0, limite)).stream()
                .sorted(Comparator.comparing(MensajeEntity::getId))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Mensaje> findDespuesDeId(Long despuesDeId, int limite) {
        return repository.findByIdGreaterThanOrderByIdAsc(despuesDeId, PageRequest.of(0, limite)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countDespuesDeId(Long despuesDeId) {
        return repository.countByIdGreaterThan(despuesDeId);
    }

    @Override
    public Long findUltimoId() {
        return repository.findUltimoId();
    }

    @Override
    public Mensaje save(Mensaje mensaje) {
        MensajeEntity guardado = repository.save(toEntity(mensaje));
        return toDomain(guardado);
    }

    private Mensaje toDomain(MensajeEntity entity) {
        return Mensaje.builder()
                .id(entity.getId())
                .usuarioCorreo(entity.getUsuarioCorreo())
                .usuarioNombre(entity.getUsuarioNombre())
                .contenido(entity.getContenido())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private MensajeEntity toEntity(Mensaje mensaje) {
        return MensajeEntity.builder()
                .id(mensaje.getId())
                .usuarioCorreo(mensaje.getUsuarioCorreo())
                .usuarioNombre(mensaje.getUsuarioNombre())
                .contenido(mensaje.getContenido())
                .creadoEn(mensaje.getCreadoEn())
                .build();
    }
}
