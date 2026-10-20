package com.viernes.equipos.application.port.out;

import com.viernes.equipos.domain.EquipoMiembro;
import java.util.List;

public interface EquipoMiembroPersistencePort {
    List<EquipoMiembro> findByEquipoId(Long equipoId);
    boolean existsByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);
    long countByEquipoId(Long equipoId);
    EquipoMiembro save(EquipoMiembro miembro);
    void deleteByEquipoId(Long equipoId);
    void deleteByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);
}
