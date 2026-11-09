package com.viernes.actividades.application.port.out;

import com.viernes.actividades.domain.Actividad;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActividadPersistencePort {
    Actividad save(Actividad actividad);
    Optional<Actividad> findById(Long id);
    List<Actividad> findAll();
    List<Actividad> findByEstado(EstadoActividad estado);
    List<Actividad> findByIdCliente(Long idCliente);
    List<Actividad> findByIdLead(Long idLead);
    List<Actividad> findByIdOportunidad(Long idOportunidad);
    List<Actividad> findByEtiquetaId(Long idEtiqueta);
    List<Actividad> findByIdUsuario(String idUsuario);
    List<Actividad> findByTipo(TipoActividad tipo);
    List<Actividad> findVencidas(EstadoActividad estado, LocalDateTime ahora);
    void deleteById(Long id);
}
