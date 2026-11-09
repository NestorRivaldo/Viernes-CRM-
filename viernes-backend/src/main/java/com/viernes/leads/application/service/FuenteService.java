package com.viernes.leads.application.service;

import com.viernes.leads.application.dto.FuenteRequest;
import com.viernes.leads.application.dto.FuenteResponse;
import com.viernes.leads.application.port.in.ManageFuenteUseCase;
import com.viernes.leads.application.port.out.FuentePersistencePort;
import com.viernes.leads.domain.Fuente;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuenteService implements ManageFuenteUseCase {

    private final FuentePersistencePort persistencePort;

    @Override
    @Cacheable(cacheNames = "fuentes", key = "#activa == null ? 'all' : #activa.toString()")
    public List<FuenteResponse> listar(Boolean activa) {
        List<Fuente> fuentes = activa != null ? persistencePort.findByActiva(activa) : persistencePort.findAll();
        return fuentes.stream().map(this::toResponse).toList();
    }

    @Override
    @Cacheable(cacheNames = "fuentes", key = "#id")
    public FuenteResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "fuentes", allEntries = true)
    public FuenteResponse crear(FuenteRequest request) {
        if (persistencePort.existsByCodigo(request.codigo())) {
            throw new IllegalArgumentException("Ya existe una fuente con el código: " + request.codigo());
        }
        Fuente f = Fuente.builder()
            .creadoEn(LocalDateTime.now())
            .activa(request.activa() != null ? request.activa() : true)
            .build();
        apply(f, request);
        return toResponse(persistencePort.save(f));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "fuentes", allEntries = true)
    public FuenteResponse actualizar(Long id, FuenteRequest request) {
        Fuente f = findOrThrow(id);
        apply(f, request);
        return toResponse(persistencePort.save(f));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "fuentes", allEntries = true)
    public void eliminar(Long id) {
        findOrThrow(id);
        persistencePort.deleteById(id);
    }

    private Fuente findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuente no encontrada: " + id));
    }

    private void apply(Fuente f, FuenteRequest r) {
        f.setCodigo(r.codigo());
        f.setNombre(r.nombre());
        f.setDescripcion(r.descripcion());
        if (r.activa() != null) f.setActiva(r.activa());
    }

    private FuenteResponse toResponse(Fuente f) {
        return new FuenteResponse(f.getId(), f.getCodigo(), f.getNombre(),
                f.getDescripcion(), f.getActiva(), f.getCreadoEn());
    }
}
