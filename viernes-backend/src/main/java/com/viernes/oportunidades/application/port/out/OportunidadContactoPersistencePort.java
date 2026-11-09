package com.viernes.oportunidades.application.port.out;

import com.viernes.oportunidades.domain.OportunidadContacto;
import java.util.List;
import java.util.Optional;

public interface OportunidadContactoPersistencePort {
    OportunidadContacto save(OportunidadContacto contacto);
    List<OportunidadContacto> findByIdOportunidad(Long idOportunidad);
    Optional<OportunidadContacto> findByIdOportunidadAndIdContacto(Long idOportunidad, Long idContacto);
    boolean existsByIdOportunidadAndIdContacto(Long idOportunidad, Long idContacto);
    void delete(OportunidadContacto contacto);
    void deleteByIdOportunidad(Long idOportunidad);
}
