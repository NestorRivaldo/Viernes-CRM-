package com.viernes.actividades.application.port.out;

import com.viernes.actividades.domain.ActividadEtiqueta;

import java.util.Collection;
import java.util.List;

public interface ActividadEtiquetaPersistencePort {
    List<ActividadEtiqueta> findByIdActividad(Long idActividad);

    List<ActividadEtiqueta> findByIdActividadIn(Collection<Long> idsActividad);

    void reemplazarEtiquetas(Long idActividad, Collection<Long> idsEtiqueta);

    void deleteByIdActividad(Long idActividad);
}
