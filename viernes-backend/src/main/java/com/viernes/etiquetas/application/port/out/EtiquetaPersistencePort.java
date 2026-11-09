package com.viernes.etiquetas.application.port.out;

import com.viernes.etiquetas.domain.Etiqueta;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EtiquetaPersistencePort {
    List<Etiqueta> findAll();

    List<Etiqueta> findByActiva(Boolean activa);

    Optional<Etiqueta> findById(Long id);

    List<Etiqueta> findAllById(Collection<Long> ids);

    boolean existsByNombreNormalizado(String nombreNormalizado);

    boolean existsByNombreNormalizadoAndIdNot(String nombreNormalizado, Long id);

    Etiqueta save(Etiqueta etiqueta);
}
