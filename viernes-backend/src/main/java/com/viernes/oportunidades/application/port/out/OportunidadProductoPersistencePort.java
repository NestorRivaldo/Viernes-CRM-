package com.viernes.oportunidades.application.port.out;

import com.viernes.oportunidades.domain.OportunidadProducto;
import java.util.List;
import java.util.Optional;

public interface OportunidadProductoPersistencePort {
    OportunidadProducto save(OportunidadProducto producto);
    List<OportunidadProducto> findByIdOportunidad(Long idOportunidad);
    Optional<OportunidadProducto> findByIdOportunidadAndIdProducto(Long idOportunidad, Long idProducto);
    boolean existsByIdOportunidadAndIdProducto(Long idOportunidad, Long idProducto);
    void delete(OportunidadProducto producto);
    void deleteByIdOportunidad(Long idOportunidad);
}
