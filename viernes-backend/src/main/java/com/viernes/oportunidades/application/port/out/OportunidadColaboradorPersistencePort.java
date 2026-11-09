package com.viernes.oportunidades.application.port.out;

import com.viernes.oportunidades.domain.OportunidadColaborador;
import java.util.List;
import java.util.Optional;

public interface OportunidadColaboradorPersistencePort {
    OportunidadColaborador save(OportunidadColaborador colaborador);
    List<OportunidadColaborador> findByIdOportunidad(Long idOportunidad);
    Optional<OportunidadColaborador> findByIdOportunidadAndIdUsuario(Long idOportunidad, Long idUsuario);
    boolean existsByIdOportunidadAndIdUsuario(Long idOportunidad, Long idUsuario);
    void delete(OportunidadColaborador colaborador);
    void deleteByIdOportunidad(Long idOportunidad);
}
