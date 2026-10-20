package com.viernes.equipos.adapters.out.persistence.repository;

import com.viernes.equipos.adapters.out.persistence.entity.EquipoEntity;
import com.viernes.equipos.domain.EstadoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipoSpringDataRepository extends JpaRepository<EquipoEntity, Long> {
    List<EquipoEntity> findByEstado(EstadoEquipo estado);
    boolean existsByNombre(String nombre);
}
