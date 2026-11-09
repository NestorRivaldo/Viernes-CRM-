package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.oportunidades.adapters.out.persistence.entity.OportunidadProductoEntity;
import com.viernes.oportunidades.adapters.out.persistence.repository.OportunidadProductoSpringDataRepository;
import com.viernes.oportunidades.application.port.out.OportunidadProductoPersistencePort;
import com.viernes.oportunidades.domain.OportunidadProducto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OportunidadProductoPersistenceAdapter implements OportunidadProductoPersistencePort {

    private final OportunidadProductoSpringDataRepository repository;

    @Override
    public OportunidadProducto save(OportunidadProducto producto) {
        return toDomain(repository.save(toEntity(producto)));
    }

    @Override
    public List<OportunidadProducto> findByIdOportunidad(Long idOportunidad) {
        return repository.findByIdOportunidad(idOportunidad).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<OportunidadProducto> findByIdOportunidadAndIdProducto(Long idOportunidad, Long idProducto) {
        return repository.findByIdOportunidadAndIdProducto(idOportunidad, idProducto).map(this::toDomain);
    }

    @Override
    public boolean existsByIdOportunidadAndIdProducto(Long idOportunidad, Long idProducto) {
        return repository.existsByIdOportunidadAndIdProducto(idOportunidad, idProducto);
    }

    @Override
    public void delete(OportunidadProducto producto) {
        repository.deleteById(producto.getId());
    }

    @Override
    public void deleteByIdOportunidad(Long idOportunidad) {
        repository.deleteByIdOportunidad(idOportunidad);
    }

    private OportunidadProducto toDomain(OportunidadProductoEntity entity) {
        return OportunidadProducto.builder()
                .id(entity.getId())
                .idOportunidad(entity.getIdOportunidad())
                .idProducto(entity.getIdProducto())
                .cantidad(entity.getCantidad())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    private OportunidadProductoEntity toEntity(OportunidadProducto domain) {
        return OportunidadProductoEntity.builder()
                .id(domain.getId())
                .idOportunidad(domain.getIdOportunidad())
                .idProducto(domain.getIdProducto())
                .cantidad(domain.getCantidad())
                .creadoEn(domain.getCreadoEn())
                .build();
    }
}
