package com.viernes.propuestas.application.port.out;

import com.viernes.propuestas.domain.EstadoPropuesta;
import com.viernes.propuestas.domain.Propuesta;
import java.util.List;
import java.util.Optional;

public interface PropuestaPersistencePort {
    Propuesta save(Propuesta propuesta);
    Optional<Propuesta> findById(Long id);
    List<Propuesta> findAll();
    List<Propuesta> findByIdOportunidad(Long idOportunidad);
    List<Propuesta> findByIdCliente(Long idCliente);
    List<Propuesta> findByEstado(EstadoPropuesta estado);
    long countByIdOportunidad(Long idOportunidad);
    long countByNumeroPropuestaStartsWith(String prefijo);
    void deleteById(Long id);
}
