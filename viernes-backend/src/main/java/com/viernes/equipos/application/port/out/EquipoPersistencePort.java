package com.viernes.equipos.application.port.out;

import com.viernes.equipos.domain.Equipo;
import com.viernes.equipos.domain.EstadoEquipo;
import java.util.List;
import java.util.Optional;

public interface EquipoPersistencePort {
    List<Equipo> findAll();
    List<Equipo> findByEstado(EstadoEquipo estado);
    Optional<Equipo> findById(Long id);
    boolean existsByNombre(String nombre);
    Equipo save(Equipo equipo);
    void deleteById(Long id);
}
