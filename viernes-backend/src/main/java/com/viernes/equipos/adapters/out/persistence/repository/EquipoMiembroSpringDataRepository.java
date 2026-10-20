package com.viernes.equipos.adapters.out.persistence.repository;

import com.viernes.equipos.adapters.out.persistence.entity.EquipoMiembroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipoMiembroSpringDataRepository extends JpaRepository<EquipoMiembroEntity, Long> {
    List<EquipoMiembroEntity> findByEquipoId(Long equipoId);
    boolean existsByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);
    long countByEquipoId(Long equipoId);
    void deleteByEquipoId(Long equipoId);
    void deleteByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);
}
